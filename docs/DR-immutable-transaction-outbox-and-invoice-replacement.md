# Design Review: immutable transaction outbox and invoice replacement

## Status

Proposed.

## Context

`idpay-payment` owns the authoritative payment transaction, invoice and credit
note commands, and invoice blob lifecycle. `idpay-transactions` owns
reward-batch membership and in-batch evaluation state.

The payment transaction lifecycle includes:

```text
CAPTURED --initial invoice--> INVOICED
INVOICED --replace invoice--> INVOICED
INVOICED --credit note------> REFUNDED
```

Invoice replacement is not a new transaction state. It is a distinct operation
that changes canonical invoice data while the resulting transaction status
remains `INVOICED`.

The current `transaction_outbox` has this identity:

```text
UNIQUE (transaction_id, event_type)
```

and handles conflicts by updating the existing payload and creation time. This
is unsafe because separate committed outcomes can overwrite the same outbox
row. It weakens event identity, auditability, retry stability, and revision
ordering.

An earlier design considered publishing two events for invoice replacement:

1. a generic `TRANSACTION_INVOICED` snapshot; and
2. a dedicated `INVOICE_REPLACED` impact event.

That fan-out would require `idpay-transactions` to coordinate two independently
delivered records for the same revision. Kafka partitions, retries, or separate
consumer paths could temporarily apply the canonical projection and the
reward-batch effect at different times.

## Decision

### 1. Publish one event for each transaction revision

Every published canonical transaction revision produces exactly one immutable
outbox record and one Kafka message.

The event type describes the committed operation, while `status` in the
canonical transaction snapshot describes the resulting state:

| Payment outcome | Event type | Resulting status |
| --- | --- | --- |
| Initial invoice | `TRANSACTION_INVOICED` | `INVOICED` |
| Invoice replacement | `TRANSACTION_INVOICE_REPLACED` | `INVOICED` |
| Invoiced reversal | `TRANSACTION_REFUNDED` | `REFUNDED` |

`TRANSACTION_INVOICE_REPLACED` replaces both the generic
`TRANSACTION_INVOICED` snapshot and the separate `INVOICE_REPLACED` impact for
that revision. It carries the complete canonical post-operation transaction
snapshot, so all consumers can update payment-owned projection fields from the
same message.

Reversal continues to use `TRANSACTION_REFUNDED`. The receiver persists the
canonical `REFUNDED` snapshot and idempotently detaches any current
reward-batch membership in the same local transaction.

No `INVOICE_REPLACED` or `INVOICED_REVERSED` secondary stream is required.

### 2. Event type must not overwrite transaction operation type

The new classification belongs in the outbox `event_type` column and in the
serialized outbox payload field `eventType`.

The existing `RewardTransactionDTO.operationType` field has payment-domain
semantics and must not be overwritten with values such as
`TRANSACTION_INVOICE_REPLACED`.

`eventType` is event metadata. It must not be added to the authoritative
transaction table or transaction entity because it describes a committed
occurrence, not transaction state. The outbox writer adds it while serializing
the immutable event payload, and consumers deserialize it alongside the
canonical transaction fields.

### 3. Every outbox event is immutable and append-only

An outbox row represents one committed fact. After insertion, its identity and
payload must never change.

The `transaction_outbox` must:

- insert one row per published transaction revision;
- never update an existing event row;
- retain the exact serialized payload created for that revision;
- use a stable identity for CDC, tracing, and retries; and
- allow controlled retention deletion without permitting event mutation.

The existing V1 migration must not be edited after deployment. A forward-only
Flyway migration must:

1. add `transaction_revision`;
2. add `schema_version` and `occurred_at` if they are not carried reliably by
   the existing row and payload;
3. backfill legacy rows with the revision present in the payload, or `0` when
   absent;
4. drop `UNIQUE (transaction_id, event_type)`;
5. add `UNIQUE (transaction_id, transaction_revision)`;
6. replace `ON CONFLICT ... DO UPDATE` with insert-only behavior; and
7. prevent application and CDC roles from updating event rows.

The generated numeric outbox `id` can be the event identity. A separate UUID is
unnecessary unless required by the cross-service contract.

`DELETE` is not part of event publication. If retention is introduced, it must
use a dedicated maintenance role and a documented age or archival rule.

Publisher bookkeeping such as claims, attempts, leases, errors, or publication
timestamps must not mutate the immutable event record. If this bookkeeping is
required, it belongs in a separate delivery table keyed by the outbox event
ID.

### 4. Use one versioned event payload

The outbox payload and Kafka message are one flat JSON object containing event
metadata plus all fields from the canonical post-operation
`RewardTransactionDTO`:

```text
eventId: stable identifier of the committed event
schemaVersion: 1
eventType: TRANSACTION_INVOICED
         | TRANSACTION_INVOICE_REPLACED
         | TRANSACTION_REFUNDED
occurredAt: timestamp of the committed outcome
transactionRevision: transaction lifecycle revision
...complete canonical post-operation RewardTransactionDTO fields
```

The transaction fields:

- contains exactly one initiative;
- contains only payment-owned canonical data;
- contains the authoritative merchant and point-of-sale information;
- contains the resulting status; and
- does not contain reward-batch membership or evaluation fields.

Publication retries reuse the stored event ID and serialized payload. A retry
must not rebuild the event from the current transaction row.

The event-only fields are materialized in `transaction_outbox.payload`, not in
the authoritative transaction table. The outbox `event_type` column and
payload `eventType` must match. One outbox row and one Kafka message remain the
invariant.

### 5. Transaction revision is the ordering and idempotency boundary

`transactionRevision` is transaction-scoped and monotonically increasing. It
is independent of `counterVersion`, Kafka offsets, timestamps, and outbox IDs.

Every canonical payment change that is published must atomically increment the
revision exactly once. The transaction update and its single outbox row use the
same new revision.

The current read-modify-write helper is not sufficient concurrency control.
Two concurrent invoice replacements could load revision `N`, both calculate
`N + 1`, and overwrite one another.

The command must use one of:

- JPA optimistic locking with a dedicated `@Version` field and a separately
  managed transaction revision; or
- a conditional database update that succeeds only when the persisted
  revision equals the expected revision.

Only one concurrent command may commit a given next revision. The losing
command receives an explicit conflict and must not create an outbox row.

### 6. Database outcome and event insertion are atomic

For a successful invoice replacement, one PostgreSQL transaction must:

1. lock or conditionally update the authoritative transaction;
2. revalidate its current status and revision;
3. set the new invoice data and update timestamp;
4. increment `transactionRevision`; and
5. insert one immutable `TRANSACTION_INVOICE_REPLACED` outbox row containing
   the canonical post-operation snapshot.

Either all database changes commit or none of them commit. Kafka publication
is never performed inside this transaction.

The application command should explicitly select
`TRANSACTION_INVOICE_REPLACED`, because it knows the business operation being
executed. If the existing database trigger remains responsible for outbox
insertion, it must distinguish:

```text
CAPTURED -> INVOICED                         = TRANSACTION_INVOICED
INVOICED -> INVOICED with changed invoice  = TRANSACTION_INVOICE_REPLACED
REWARDED -> INVOICED with changed invoice  = TRANSACTION_INVOICE_REPLACED
* -> REFUNDED                               = TRANSACTION_REFUNDED
```

The trigger must insert once for the committed revision and must never update
an existing outbox row.

### 7. Kafka provides per-transaction ordering

All transaction events use `transaction_id` as the Kafka message key. Kafka
therefore preserves ordering for one transaction within a topic partition.
This is per-transaction ordering, not global total ordering.

The ordering guarantee requires:

- one topic for these transaction events;
- the same key derivation for every event;
- no producer path that republishes the same revision to a second topic;
- one receiver pipeline for canonical projection and reward-batch effects; and
- acknowledgement only after the receiver's local SQL transaction commits.

Changing the topic partition count does not change key-based ordering for new
records, but operational migration and replay procedures must avoid combining
independently ordered histories without revision checks.

### 8. Receiver processing is atomic and revision-aware

`idpay-transactions` processes each unified event in one local SQL transaction.
It stores one latest applied transaction revision.

For each event:

1. ignore an equal or lower revision;
2. update payment-owned canonical projection fields;
3. apply the event-specific reward-batch effect;
4. persist the new latest revision; and
5. commit before acknowledging the Kafka record.

Event-specific effects are:

| Event type | Local reward-batch effect |
| --- | --- |
| `TRANSACTION_INVOICED` | Persist the initial invoiced projection; normal assignment remains owned by the existing assignment flow |
| `TRANSACTION_INVOICE_REPLACED` | Persist the projection and apply the replacement policy using membership at handling time |
| `TRANSACTION_REFUNDED` | Persist `REFUNDED` and detach current membership idempotently |

This removes the need for a separate invoice-impact watermark and eliminates
same-revision synchronization across two consumer streams.

### 9. Blob replacement favors a valid database pointer

Blob storage and PostgreSQL cannot participate in one atomic transaction. The
flow must avoid leaving the committed transaction pointing to a deleted blob.

The replacement sequence is:

1. validate the request and perform the read-only eligibility preflight;
2. upload the new invoice under an immutable, revision-aware or
   operation-unique path;
3. execute the atomic PostgreSQL transaction;
4. after commit, delete the previous invoice blob; and
5. if the database transaction fails, delete or schedule cleanup of the newly
   uploaded unreferenced blob.

The previous blob must not be deleted before the new transaction state commits.
Reusing the same storage path is not allowed because it would mutate the object
referenced by an earlier committed event payload.

### 10. Reward-batch policy has an explicit financial cutoff

The eligibility query is a preflight, not a distributed lock. Reward-batch
membership or batch state can change before the unified event is consumed. The
receiver evaluates the membership that exists when it processes
`TRANSACTION_INVOICE_REPLACED`.

Recommended policy:

| Membership at handling time | Effect |
| --- | --- |
| No membership | Persist the newer canonical projection; no batch effect |
| Source batch `CREATED` | Keep membership and in-batch state |
| Re-evaluable, non-financially-committed batch | Move to the outcome-month grouping and mark `SUSPENDED` |
| `PENDING_REFUND` or `REFUNDED` | Reject/quarantine for explicit adjustment handling |

A financially committed or completed refund must not be rewritten by removing
the transaction from its historical batch. If replacement must be supported
after that cutoff, it requires a separate adjustment or compensation model.

The exact treatment of `APPROVED` and `NOT_REFUNDED` must be agreed with the
owners of report generation and refund delivery:

- if generated reports are immutable, `APPROVED` is also beyond the cutoff;
- if reports can be regenerated before delivery, the transaction may be moved
  and suspended under an explicit rule; and
- `NOT_REFUNDED` may be re-evaluable only if the failed refund attempt remains
  preserved as immutable history.

## Rollout

The consumer is deployed before the producer.

1. Agree and version the unified event contract and financial cutoff.
2. Inventory existing consumers of `idpay-transaction`, especially consumers
   that reject unknown event types or expect a raw `RewardTransactionDTO`.
3. Deploy receiver support for `TRANSACTION_INVOICE_REPLACED`, atomic
   projection/effect handling, and revision idempotency with consumption
   disabled.
4. Deploy the payment migration that makes `transaction_outbox` append-only
   and revision-keyed. Verify the CDC pipeline publishes inserts and preserves
   the outbox event type.
5. Deploy payment production of `TRANSACTION_INVOICE_REPLACED` behind a
   feature flag.
6. Enable the receiver and then enable a controlled payment canary.
7. Exercise duplicate delivery, stale revisions, concurrent replacement,
   process restart, and month-boundary retries.
8. Enable production only after outbox backlog, conflict, quarantine, and blob
   cleanup monitoring are available.

## Required tests

- Initial invoicing produces one `TRANSACTION_INVOICED` event.
- Each successful replacement produces one
  `TRANSACTION_INVOICE_REPLACED` event and no additional
  `TRANSACTION_INVOICED` event for the same revision.
- Two successful replacements produce two revisions and two immutable outbox
  rows.
- No outbox row can be updated.
- A failed database transaction persists neither transaction changes nor an
  outbox row.
- Concurrent replacements cannot commit the same revision.
- A publisher retry uses the same event ID and byte-equivalent payload.
- Upload failure leaves the transaction and outbox unchanged.
- Database failure after upload leaves the previous committed blob valid and
  schedules cleanup of the new unreferenced blob.
- Equal and stale revisions are idempotent no-ops.
- The receiver updates projection and batch effect atomically.
- `TRANSACTION_REFUNDED` persists `REFUNDED` and idempotently detaches current
  membership.
- Replacement at or beyond the agreed financial cutoff is rejected or
  quarantined without rewriting historical refund data.
- Existing consumers either accept or intentionally ignore
  `TRANSACTION_INVOICE_REPLACED`.

## Alternatives rejected

### Update the existing `(transaction_id, event_type)` outbox row

Rejected because it overwrites committed event history and makes multiple
replacements share one unstable identity and payload.

### Publish a generic snapshot and a separate impact event

Rejected because it creates two records for one revision and forces the
receiver to coordinate independently delivered messages and watermarks.

### Infer replacement from repeated `TRANSACTION_INVOICED` events

Rejected because initial invoice and replacement have the same resulting
status and delivery can be duplicated or replayed.

### Add a `REINVOICED` transaction status

Rejected because replacement does not change the payment lifecycle state and
would mix an operation occurrence with durable transaction state.

### Overwrite `RewardTransactionDTO.operationType`

Rejected because that payload field already has payment-domain meaning. Event
classification belongs in outbox/Kafka metadata.

### Let payment update reward-batch membership directly

Rejected because it violates service ownership and creates distributed write
coupling between payment state and reward-batch state.

### Move transactions out of already refunded batches

Rejected as the default because it rewrites the composition and derived
amounts of financially completed history. This requires an explicit adjustment
domain if the business needs to support it.

## Consequences

Positive consequences:

- one committed revision maps to one immutable event;
- transaction-key ordering is preserved through one Kafka stream;
- canonical projection and reward-batch effects are applied atomically;
- repeated invoice replacements cannot overwrite each other;
- one receiver revision watermark handles ordering and idempotency;
- payment and reward-batch ownership remain separated; and
- refund history is not silently rewritten.

Costs:

- existing consumers must understand or intentionally ignore the new event
  type;
- adding event metadata to the flat payload requires a compatibility rollout;
- delivery bookkeeping must be separated from immutable event data;
- orphan-blob cleanup and event-quarantine operations are required; and
- the business must define the exact replacement cutoff around approval and
  refund processing.

## Follow-up documentation

When this review is accepted:

1. update `docs/idpay-payment-reward-batch-impact.md` to describe the unified
   `TRANSACTION_INVOICE_REPLACED` event and generic
   `TRANSACTION_REFUNDED` reversal;
2. revise
   `docs/idpay-payment-reward-batch-impact-implementation-plan.md` so one
   immutable outbox row is produced per revision and delivery state is stored
   separately; and
3. update `specs/asyncapi.yml` with the versioned flat event payload, the new
   event type, transaction key, and compatibility rules.
