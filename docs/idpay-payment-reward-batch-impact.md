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

The first rollout preserves all fields of `RewardTransactionDTO` in one flat
JSON message and adds event-only metadata while writing
`transaction_outbox.payload`. It does not introduce a payload envelope.

| Wire element | Representation |
| --- | --- |
| Kafka key | Transaction ID from outbox `transaction_id` |
| Event type | Payload `eventType`, equal to outbox `event_type` |
| Event ID | Payload `eventId`, sourced from the immutable outbox row ID |
| Schema version | Payload `schemaVersion`, initially `1` |
| Occurrence time | Payload `occurredAt`, sourced from outbox `occurred_at` |
| Transaction revision | Required positive payload `transactionRevision`, equal to outbox `transaction_revision` |
| Canonical outcome | `RewardTransactionDTO.status` and the remaining payment-owned snapshot fields |

The two payload fields have different meanings:

- payload `eventType` classifies the event and contains values such
  as `TRANSACTION_INVOICED`, `TRANSACTION_INVOICE_REPLACED`, or
  `TRANSACTION_REFUNDED`;
- payload `RewardTransactionDTO.operationType` remains the original
  payment-domain operation code and must not be overwritten with an event
  type.

`occurredAt` is the committed outcome timestamp used for deterministic
outcome-month grouping.

`eventType`, `eventId`, `schemaVersion`, and `occurredAt` exist only in the
immutable outbox payload. They are not columns or fields of the authoritative
transaction table/entity. Consumers that classify these outcomes must
deserialize `eventType`.

For `TRANSACTION_INVOICE_REPLACED`, the payload status is `INVOICED`. For
`TRANSACTION_REFUNDED`, the payload status is `REFUNDED`.

The currently deployed PostgreSQL CDC connector does not yet satisfy this
frozen representation: it filters a closed `event_type` allowlist, extracts
`user_id` as the key, and then publishes `payload`. Before replacement
production is enabled:

1. allow `TRANSACTION_INVOICE_REPLACED`;
2. extract `transaction_id` as the Kafka key; and
3. ensure the outbox writer has already included the frozen event metadata in
   `payload` before CDC extracts it.

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
topic name `idpay-transaction`, event type `TRANSACTION_INVOICED`, and payload
transaction models.

| Repository / integration | Current behavior | Compatibility result | Required action |
| --- | --- | --- | --- |
| `pagopa/cstar-securehub-infra` PostgreSQL `transaction_connector.json` | Uses a closed `event_type` allowlist and keys by `user_id`; it already extracts the complete outbox `payload` | **Blocking** | Add the replacement type and key by `transaction_id` |
| `pagopa/idpay-transactions` | Deserializes the canonical transaction fields and persists revision-ordered snapshots, but does not deserialize `eventType` or atomically apply replacement effects | **Blocking** | Deserialize payload `eventType` and implement `TRANSACTION_INVOICE_REPLACED` handling as defined by PR 02 |
| `pagopa/idpay-ranker` | Consumes `idpay-transaction` and deserializes payload status into a closed `SyncTrxStatus` enum that does not contain `INVOICED` | **Blocking: rejects the payload** | Ignore invoice `eventType` values before closed-enum transaction processing or otherwise isolate the consumer |
| `pagopa/idpay-reward-calculator` | Deserializes the raw transaction payload with string status; its counter-unlock mediator accepts only `AUTHORIZED`, `REWARDED`, and `REJECTED` | Compatible: safely ignores `INVOICED` | No code change required for the replacement event |

Other organization search matches were producers, infrastructure references,
error-topic examples, or contracts rather than consumers of payment
transaction outcomes. No additional consumer with a closed event-type enum was
identified.

## Rollout gate

PR 04, which starts producing `TRANSACTION_INVOICE_REPLACED`, must not merge
until all blocking entries in the compatibility inventory are resolved and
deployed:

- the CDC connector publishes the frozen transaction key and payload;
- `idpay-transactions` handles the replacement event atomically; and
- `idpay-ranker` no longer rejects invoice payloads from this topic.

Receiver compatibility must be deployed before producer enablement. The
replacement producer must remain disabled until the gate is explicitly
confirmed.

## Change log

| PR | Change |
| --- | --- |
| PR 01 | Froze the unified event matrix, one-event-per-revision invariant, `transactionId` key, payload `eventType`, compatibility inventory, and PR 04 rollout gate |
