# idpay-payment impact: reward-batch invoice lifecycle

## Purpose and ownership

This document is the authoritative cross-service contract for payment-driven
invoice publication, invoice replacement, and invoiced reversal.

`idpay-payment` owns payment state, invoice and credit-note commands, and
invoice blobs. `idpay-transactions` owns reward-batch membership and in-batch
evaluation state. Payment events contain only payment-owned canonical fields;
they never carry or update reward-batch membership data.

The baseline required by this contract is present on the repositories'
`develop` branches:

- `idpay-payment` persists and publishes `transactionRevision` and performs
  the read-only reward-batch eligibility preflight;
- `idpay-transactions` persists generic snapshots in revision order; and
- `idpay-transactions` persists `REFUNDED` snapshots and idempotently detaches
  current reward-batch membership.

`transactionRevision` is independent from reward-calculator
`counterVersion`.

## Authoritative publication matrix

| Outcome | Event type | Payload status |
| --- | --- | --- |
| Initial invoice | `TRANSACTION_INVOICED` | `INVOICED` |
| Invoice replacement | `TRANSACTION_INVOICE_REPLACED` | `INVOICED` |
| Reversal | `TRANSACTION_REFUNDED` | `REFUNDED` |

An invoice replacement produces only
`TRANSACTION_INVOICE_REPLACED`. It does not also produce
`TRANSACTION_INVOICED`, `INVOICE_REPLACED`, `INVOICED_REVERSED`, or any other
secondary impact event for the same revision. A reversal produces only
`TRANSACTION_REFUNDED`.

## Unified event contract

### Identity, ordering, and topic

- Exactly one immutable event exists for each
  `(transactionId, transactionRevision)`.
- `transactionRevision` is positive and monotonically increasing within one
  transaction.
- Retries preserve the same event identity, revision, key, event type, and
  payload.
- Stale or duplicate revisions must not reapply a transaction outcome.
- All event types use the existing `idpay-transaction` topic. No second topic
  or dedicated impact outbox is introduced.
- The Kafka message key is `transactionId`, sourced from the outbox
  `transaction_id` column.

### First-rollout wire representation

The first rollout preserves the complete `RewardTransactionDTO` as the JSON
message payload. It does not introduce a payload envelope.

| Wire element | Representation |
| --- | --- |
| Kafka key | Transaction ID from outbox `transaction_id` |
| Event type | Kafka header `operationType`, sourced from outbox `event_type` |
| Event ID | Kafka header `eventId`, sourced from the immutable outbox row ID |
| Schema version | Kafka header `schemaVersion`, initially `1` |
| Occurrence time | Kafka header `occurredAt`, sourced from outbox `occurred_at` |
| Transaction revision | Kafka header `transactionRevision` and required positive `RewardTransactionDTO.transactionRevision` |
| Canonical outcome | `RewardTransactionDTO.status` and the remaining payment-owned snapshot fields |

The exact event-type header name is `operationType`. This is the existing
Spring Cloud Stream/Kafka event-classification convention used by deployed
IdPay integrations.

The header and payload fields have different meanings:

- Kafka header `operationType` classifies the event and contains values such
  as `TRANSACTION_INVOICED`, `TRANSACTION_INVOICE_REPLACED`, or
  `TRANSACTION_REFUNDED`;
- payload `RewardTransactionDTO.operationType` remains the original
  payment-domain operation code and must not be overwritten with an event
  type.

Header `transactionRevision` and payload
`RewardTransactionDTO.transactionRevision` must contain the same value.
`occurredAt` is the committed outcome timestamp used for deterministic
outcome-month grouping.

For `TRANSACTION_INVOICE_REPLACED`, the payload status is `INVOICED`. For
`TRANSACTION_REFUNDED`, the payload status is `REFUNDED`.

The currently deployed PostgreSQL CDC connector does not yet satisfy this
frozen representation: it filters a closed `event_type` allowlist, extracts
`user_id` as the key, and then extracts only `payload`, which drops
`event_type`. Before replacement production is enabled, the connector must:

1. allow `TRANSACTION_INVOICE_REPLACED`;
2. extract `transaction_id` as the Kafka key; and
3. copy `id`, `schema_version`, `occurred_at`, `transaction_revision`, and
   `event_type` to headers `eventId`, `schemaVersion`, `occurredAt`,
   `transactionRevision`, and `operationType` before extracting the payload.

These connector changes are rollout prerequisites, not part of this
documentation-only PR.

## Invoice replacement policy

The payment command keeps the existing eligibility check in
`invoiceTransaction`. The eligibility response is a read-only preflight; it
does not reserve or lock reward-batch state and must not be copied into the
event as a membership precondition.

When `idpay-transactions` handles
`TRANSACTION_INVOICE_REPLACED`, it applies the canonical projection and the
reward-batch effect in one local SQL transaction, using membership and batch
state observed at handling time:

| Membership observed at handling time | Effect |
| --- | --- |
| No membership | Persist the canonical projection; no batch movement |
| Source batch `CREATED` | Keep current membership and in-batch state |
| Any other supported current source state | Move to the outcome-month grouping and set target membership to `SUSPENDED` |

The outcome month is derived from event occurrence time in `Europe/Rome`.
Reward-batch counters remain derived from committed membership rows; payment
does not update them.

## Consumer compatibility inventory

The inventory was performed against the PagoPA GitHub organization using the
topic name `idpay-transaction`, event type `TRANSACTION_INVOICED`, and header
name `operationType`.

| Repository / integration | Current behavior | Compatibility result | Required action |
| --- | --- | --- | --- |
| `pagopa/cstar-securehub-infra` PostgreSQL `transaction_connector.json` | Uses a closed `event_type` allowlist, keys by `user_id`, and drops outbox metadata when extracting `payload` | **Blocking** | Add the replacement type, key by `transaction_id`, and emit the frozen metadata headers |
| `pagopa/idpay-transactions` | Deserializes the complete `RewardTransactionDTO` and persists revision-ordered snapshots, but does not classify or atomically apply replacement effects | **Blocking** | Implement `TRANSACTION_INVOICE_REPLACED` handling before production, as defined by PR 02 |
| `pagopa/idpay-ranker` | Consumes `idpay-transaction` without checking the event-type header and deserializes payload status into a closed `SyncTrxStatus` enum that does not contain `INVOICED` | **Blocking: rejects the payload** | Ignore invoice event types before closed-enum deserialization or otherwise isolate the consumer from these events |
| `pagopa/idpay-reward-calculator` | Deserializes the raw transaction payload with string status; its counter-unlock mediator accepts only `AUTHORIZED`, `REWARDED`, and `REJECTED` | Compatible: safely ignores `INVOICED` | No code change required for the replacement event |

Other organization search matches were producers, infrastructure references,
error-topic examples, or contracts rather than consumers of payment
transaction outcomes. No additional consumer with a closed event-type enum was
identified.

## Rollout gate

PR 04, which starts producing `TRANSACTION_INVOICE_REPLACED`, must not merge
until all blocking entries in the compatibility inventory are resolved and
deployed:

- the CDC connector publishes the frozen key and `operationType` header;
- `idpay-transactions` handles the replacement event atomically; and
- `idpay-ranker` no longer rejects invoice payloads from this topic.

Receiver compatibility must be deployed before producer enablement. The
replacement producer must remain disabled until the gate is explicitly
confirmed.

## Change log

| PR | Change |
| --- | --- |
| PR 01 | Froze the unified event matrix, one-event-per-revision invariant, `transactionId` key, `operationType` event header, compatibility inventory, and PR 04 rollout gate |
