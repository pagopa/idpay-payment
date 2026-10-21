# Implementation Plan: immutable unified transaction events

**Source design review:**  
[DR-immutable-transaction-outbox-and-invoice-replacement.md](./DR-immutable-transaction-outbox-and-invoice-replacement.md)

## Purpose

This document is an execution contract for coding agents. It divides the
design review into small, self-contained pull requests that can be deployed
progressively.

After this plan is accepted, the user should only need to issue commands in
this form:

```text
implement PR 01
implement PR 02
...
```

The coding agent must use the corresponding section as the complete task
definition. It must not require the user to repeat repository, branch, scope,
test, or rollout instructions already recorded here.

## Global execution protocol

### Branch and pull-request rules

Every implementation PR must:

1. target the repository named in its section;
2. start only after all listed prerequisites are merged;
3. fetch the current remote state;
4. create a new branch directly from `origin/develop`;
5. use `develop` as the pull-request base;
6. avoid stacked branches and PRs whose base is another feature branch;
7. include only the scope assigned to that PR; and
8. update this plan's progress table in the same PR only when explicitly
   required by that PR.

The branch setup is:

```bash
git fetch origin
git switch -c <branch-name> origin/develop
```

If the branch already exists remotely, the agent must inspect it before
continuing and must not overwrite or rewrite its history. If the worktree
contains unrelated changes, the agent must preserve them and create the branch
in a clean worktree or ask the user only when safe isolation is impossible.

The PR must be opened against:

```text
base: develop
head: <branch-name>
```

The coding agent must not start the next PR before the previous prerequisite
PRs are merged into `develop`. This ensures that every new branch really
originates from the latest `origin/develop`.

### Implementation rules

For every PR, the coding agent must:

- read the source design review and this plan before editing;
- inspect current `develop` because earlier PRs may have changed file paths or
  implementation details;
- preserve service ownership:
  `idpay-payment` owns payment state and invoice blobs, while
  `idpay-transactions` owns reward-batch membership;
- keep `transactionRevision` independent from reward-calculator
  `counterVersion`;
- produce one immutable outbox event for one published transaction revision;
- keep payload `operationType` distinct from payload `eventType`;
- use `transaction_id` as the Kafka key;
- avoid a second topic or secondary invoice-impact event;
- add targeted unit and integration tests in the same PR as behavior changes;
- update directly affected contracts and documentation;
- run the repository's narrowest relevant validation first, followed by its
  normal full validation when feasible; and
- stop and report a blocker rather than silently changing the agreed
  architecture.

### Existing work that must be treated as baseline

Before PR 01 starts, the agent must inspect both repositories' `develop`
branches and confirm the following baseline capabilities exist:

- `idpay-payment` persists and publishes `transactionRevision`;
- `idpay-payment` contains the read-only reward-batch eligibility preflight;
- `idpay-transactions` persists generic snapshots with revision ordering; and
- `idpay-transactions` accepts `TRANSACTION_REFUNDED`, persists `REFUNDED`,
  and idempotently detaches current batch membership.

If one of these items is still represented by an open PR rather than merged
code, the agent must stop and identify the exact prerequisite PR. It must not
reimplement or duplicate that work inside PR 01.

### Agreed replacement policy

The payment command keeps the existing eligibility policy in
`invoiceTransaction`. That query is a preflight and does not reserve or lock
reward-batch state.

When `TRANSACTION_INVOICE_REPLACED` is eventually handled,
`idpay-transactions` uses the membership and batch state that exist at handling
time and applies the replacement effect for that current state. It does not
quarantine merely because the state changed after the payment preflight.

The current replacement effect remains:

| Membership observed at handling time | Effect |
| --- | --- |
| No membership | Persist the canonical projection; no batch movement |
| Source batch `CREATED` | Keep current membership and in-batch state |
| Any other supported current source state | Move to the outcome-month grouping and set the target membership to `SUSPENDED` |

The outcome month is derived from event `occurredAt` in `Europe/Rome`.
Reward-batch counters remain derived from committed membership rows; payment
does not update them.

## Progress table

| PR | Repository | Deliverable | Depends on | Status |
| --- | --- | --- | --- | --- |
| 01 | `pagopa/idpay-payment` | Freeze unified event contract and compatibility inventory | Baseline | Planned |
| 02 | `pagopa/idpay-transactions` | Consume and atomically apply `TRANSACTION_INVOICE_REPLACED` | PR 01 | Planned |
| 03 | `pagopa/idpay-payment` | Make `transaction_outbox` immutable and revision-keyed | PR 02 | Planned |
| 04 | `pagopa/idpay-payment` | Emit one unified replacement event atomically and concurrency-safely | PR 03 | Planned |
| 05 | `pagopa/idpay-payment` | Make invoice blob replacement failure-safe and immutable-path based | PR 04 | Planned |
| 06 | `pagopa/idpay-transactions` | Remove obsolete dedicated invoice-impact machinery | PR 04 | Planned |
| 07 | `pagopa/idpay-payment` | Finalize contracts, operational documentation, and rollout defaults | PR 05, PR 06 | Planned |

## Dependency sequence

```text
PR 01 payment contract
  -> PR 02 transactions receiver
  -> PR 03 payment immutable outbox
  -> PR 04 payment unified producer
       -> PR 05 payment blob safety
       -> PR 06 transactions cleanup
            -> PR 07 final contracts and rollout
```

PR 05 and PR 06 may be implemented in either order after PR 04 is merged, but
they must each start from their repository's current `origin/develop`. PR 07
starts only after both are merged.

---

## PR 01 - Freeze the unified event contract

**Repository:** `pagopa/idpay-payment`  
**Suggested branch:** `LPBD-unified-transaction-event-contract`  
**Base:** `origin/develop`  
**Prerequisites:** baseline capabilities confirmed

### Objective

Freeze the additive wire contract and document compatibility before any
producer emits `TRANSACTION_INVOICE_REPLACED`.

This is a contract/documentation PR. It must not change runtime event
production.

### Required changes

1. Update `docs/idpay-payment-reward-batch-impact.md` so the authoritative
   publication matrix is:

   | Outcome | Event type | Payload status |
   | --- | --- | --- |
   | Initial invoice | `TRANSACTION_INVOICED` | `INVOICED` |
   | Invoice replacement | `TRANSACTION_INVOICE_REPLACED` | `INVOICED` |
   | Reversal | `TRANSACTION_REFUNDED` | `REFUNDED` |

2. Remove the two-event design from the living contract:
   - no generic plus dedicated event for the same revision;
   - no `INVOICE_REPLACED` secondary impact;
   - no `INVOICED_REVERSED` impact.
3. Define one event per `(transactionId, transactionRevision)`.
4. Define `transactionId` as the Kafka key.
5. Define the compatibility representation for the first rollout:
   - keep all fields of `RewardTransactionDTO` in the message payload;
   - keep payment payload `operationType` unchanged;
   - add event metadata, including `eventType`, directly to the flat outbox
     payload without adding it to the transaction table or entity;
   - require payload `eventType` to match outbox `event_type`;
   - do not introduce a second topic.
6. Update `specs/asyncapi.yml` additively with
   `TRANSACTION_INVOICE_REPLACED`, revision requirements, event metadata, and
   payload status `INVOICED`.
7. Search known organization repositories for consumers of
   `idpay-transaction`, `TRANSACTION_INVOICED`, and payload `eventType`.
   Record in the contract:
   - consumers that require a code change;
   - consumers that ignore unknown event types safely; and
   - any consumer that deserializes a closed enum and would reject the new
     value.
8. Add a rollout gate stating that PR 04 cannot merge until every blocking
   consumer is compatible.

### Explicit non-goals

- No Flyway migration.
- No trigger change.
- No Java production behavior change.
- No new Kafka topic or binding.
- No dedicated impact outbox.

### Validation

Documentation-only validation is sufficient unless AsyncAPI has an existing
repository validation command. Check links, schema references, examples, and
repository formatting.

### Acceptance criteria

- The contract unambiguously defines one event for one revision.
- The payload `operationType` and event type are explicitly distinguished.
- Consumer compatibility findings are recorded.
- No runtime behavior changes.

### Deployment and rollback

No runtime deployment effect. Reverting the documentation PR is sufficient if
the contract is rejected before implementation.

---

## PR 02 - Add atomic replacement handling to `idpay-transactions`

**Repository:** `pagopa/idpay-transactions`  
**Suggested branch:** `LPBD-consume-unified-invoice-replaced`  
**Base:** `origin/develop`  
**Prerequisites:** PR 01 merged

### Objective

Deploy receiver support before the producer can emit the new event type.
`TRANSACTION_INVOICE_REPLACED` must update the canonical projection and apply
the reward-batch consequence in one local SQL transaction.

### Required changes

1. Extend the existing `idpay-transaction` consumer event classification to
   recognize `TRANSACTION_INVOICE_REPLACED` from payload `eventType` frozen in
   PR 01.
2. Keep payload `operationType` untouched and deserialize the event metadata
   together with the complete canonical `RewardTransactionDTO` fields.
3. Validate replacement events:
   - event revision is present and positive;
   - payload status is `INVOICED`;
   - exactly one initiative is present;
   - merchant and point-of-sale information required for grouping are present;
   - local reward-batch fields are not accepted from payment.
4. Reuse or extract the existing invoice-replacement SQL logic instead of
   duplicating it.
5. In one SQL transaction:
   - lock or conditionally update the local transaction projection;
   - ignore equal or stale revisions;
   - update payment-owned canonical fields;
   - read membership and source batch state at handling time;
   - keep membership unchanged for a `CREATED` source;
   - otherwise move membership to the `occurredAt` month grouping and set it
     to `SUSPENDED`;
   - do nothing to membership when no membership exists; and
   - persist the latest applied transaction revision.
6. Acknowledge the Kafka message only after the SQL transaction commits.
7. Keep existing handling for `TRANSACTION_INVOICED` and
   `TRANSACTION_REFUNDED`.
8. Do not add a second consumer binding, topic, or impact watermark.
9. Update the repository's AsyncAPI/contract documentation and deployment
   configuration only where required to recognize the new event type on the
   existing stream.

### Required tests

- Newer replacement revision with no membership updates projection only.
- `CREATED` membership is kept.
- Every other currently supported source state follows the existing move and
  suspend behavior.
- Target grouping uses `occurredAt` in `Europe/Rome`, including a month
  boundary.
- Equal revision is an idempotent no-op.
- Stale revision cannot overwrite projection or membership.
- Projection update and membership change roll back together on failure.
- Invalid status, revision mismatch, multiple initiatives, or missing grouping
  data follow the repository's explicit error/quarantine mechanism.
- Existing invoiced and refunded consumer tests remain green.

### Validation

Use the repository's targeted consumer/service tests and PostgreSQL
integration tests, followed by its documented full Maven validation. Use
Testcontainers for the atomic membership transition if that is the existing
SQL integration-test convention.

### Acceptance criteria

- Receiver can be deployed safely while payment still emits no replacement
  event.
- One revision watermark controls projection and replacement effects.
- No partial projection/membership commit is possible.
- No dedicated impact consumer is added.

### Deployment and rollback

Deploy with the existing consumer configuration. Because the producer is not
yet emitting the new type, rollback is an ordinary application rollback with
no data migration.

---

## PR 03 - Make the payment transaction outbox immutable

**Repository:** `pagopa/idpay-payment`  
**Suggested branch:** `LPBD-immutable-transaction-outbox`  
**Base:** `origin/LPBD-immutable-transaction-outbox` created from `origin/develop`  
**Prerequisites:** PR 02 merged and deployed compatibly

### Objective

Make the existing `transaction_outbox` append-only and revision-keyed without
yet changing replacement event classification.

### Required changes

1. Add a forward-only Flyway migration. Do not edit V1 or V2.
2. Add outbox columns required by the frozen contract when absent:
   - `transaction_revision`;
   - `schema_version`;
   - `occurred_at`.
3. Backfill existing rows:
   - extract `transactionRevision` from payload when present;
   - use `0` for legacy payloads without it;
   - set schema version `1`;
   - preserve original creation time as `occurred_at` when no better committed
     outcome timestamp exists;
   - add `eventId`, `schemaVersion`, `eventType`, `occurredAt`, and
     `transactionRevision` to legacy payloads from the corresponding outbox
     columns.
4. Drop `UNIQUE (transaction_id, event_type)`.
5. Add `UNIQUE (transaction_id, transaction_revision)`.
6. Change the outbox function to insert immutable rows:
   - copy `NEW.transactionRevision`;
   - serialize the committed snapshot once and add the outbox-only event
     metadata fields;
   - keep payload `eventType` equal to row `event_type`;
   - remove `DO UPDATE`;
   - use insert-only conflict handling for an exact duplicate invocation.
7. Preserve existing event classification in this PR. In particular, an
   `INVOICED -> INVOICED` or `REWARDED -> INVOICED` update may still be
   classified as `TRANSACTION_INVOICED` until PR 04.
8. Prevent ordinary application and CDC roles from updating outbox records
   using the database permission or guard mechanism consistent with deployment
   ownership. If database grants are infrastructure-managed, add an explicit
   update-rejection trigger and document the maintenance-role exception for
   retention.
9. Do not add delivery attempts, leases, errors, or publication status to the
   immutable event row.

### Required tests

- Two different revisions for one transaction create two rows.
- Two different statuses at different revisions create two rows.
- Repeating the exact same `(transaction_id, transaction_revision)` does not
  mutate the existing row.
- An attempted `UPDATE` is rejected.
- Legacy rows are backfilled and remain readable.
- Trigger payload event type and revision match the row metadata.
- Transaction rollback also rolls back the outbox insert.

### Validation

Add or extend PostgreSQL migration/trigger integration tests. Run the targeted
tests and the repository Maven test suite.

### Acceptance criteria

- No committed outbox event can be overwritten.
- CDC continues receiving inserts on the existing table.
- Existing event types remain unchanged in this PR.
- Repeated invoice operations can be represented by separate rows.

### Deployment and rollback

Deploy only after confirming CDC reads inserts and does not depend on update
events. Application rollback is allowed, but the forward migration remains.
Do not restore the mutable unique constraint.

---

## PR 04 - Emit the unified replacement event atomically

**Repository:** `pagopa/idpay-payment`  
**Suggested branch:** `LPBD-emit-unified-invoice-replaced`  
**Base:** `origin/develop`  
**Prerequisites:** PR 03 merged; PR 02 deployed; PR 01 consumer inventory has no blockers

### Objective

Change invoice replacement so one committed replacement revision creates one
`TRANSACTION_INVOICE_REPLACED` outbox event containing the complete canonical
post-operation snapshot.

### Required changes

1. Introduce a typed payment event classification that is distinct from
   payload `operationType`.
2. Classify invoice outcomes:
   - `CAPTURED -> INVOICED` as `TRANSACTION_INVOICED`;
   - `INVOICED -> INVOICED` through the invoice replacement command as
     `TRANSACTION_INVOICE_REPLACED`;
   - `REWARDED -> INVOICED` through the invoice replacement command as
     `TRANSACTION_INVOICE_REPLACED`;
   - reversal as `TRANSACTION_REFUNDED`.
3. Prefer explicit application intent over a trigger that infers replacement
   from arbitrary field differences. Refactor the transaction persistence seam
   so the command supplies the event type while the transaction update and
   outbox insert remain in one PostgreSQL transaction:
   - after validating the current transaction, the invoice command selects a
     typed event value from the pre-mutation state:
     `TRANSACTION_INVOICED` for `CAPTURED`, or
     `TRANSACTION_INVOICE_REPLACED` for `INVOICED` and `REWARDED`;
   - pass that value, the expected revision, and the new invoice data to a
     dedicated transactional persistence method;
   - in that method, conditionally update the transaction and increment its
     revision, obtain the resulting canonical row, and explicitly insert the
     immutable outbox row with `event_type` set to the supplied value;
   - build the JSON payload from that resulting row and add the same supplied
     value as payload `eventType`;
   - if the conditional update affects no row, report a conflict and do not
     attempt the outbox insert;
   - change the existing transaction trigger so it does not also emit an event
     for invoice mutations handled by this explicit persistence method.

   Do not carry the event type through `RewardTransactionDTO.operationType`, a
   new transaction-table column, or session-local database state. The event
   type is an argument to the transactional write operation and is persisted
   only in the outbox row and its payload.
4. Make the invoice database update concurrency-safe:
   - use an expected-revision conditional update or equivalent optimistic
     locking;
   - increment `transactionRevision` exactly once;
   - return an explicit conflict when another command already changed the
     expected revision;
   - never create an outbox row for the losing command.
5. Revalidate transaction ID, initiative, merchant, allowed status, and current
   revision inside the database transaction.
6. Persist one event row with:
   - stable event ID;
   - schema version `1`;
   - event type;
   - one `occurredAt`;
   - the new transaction revision;
   - the complete canonical `RewardTransactionDTO` fields;
   - all event metadata flattened into the JSON payload without adding
     `eventType` to the transaction table or entity.
7. Ensure payload `eventType` matches row `event_type` and payload revision
   matches row `transaction_revision`.
8. Use `transactionId` as the Kafka key through the existing CDC mapping.
9. Do not publish an additional `TRANSACTION_INVOICED` event for the same
   replacement revision.
10. Keep the existing eligibility preflight before mutation.
11. Update audit logging to distinguish initial invoice and replacement while
    preserving sanitized identifiers.
12. Update AsyncAPI examples and contract tests to match the actual emitted
    metadata and payload.

### Required tests

- Initial invoice emits only `TRANSACTION_INVOICED`.
- Replacement from `INVOICED` emits only
  `TRANSACTION_INVOICE_REPLACED`.
- Replacement from `REWARDED` changes the canonical status to `INVOICED` and
  emits only `TRANSACTION_INVOICE_REPLACED`.
- Consecutive replacements create consecutive revisions and distinct immutable
  rows.
- Outbox event type does not overwrite payload `operationType`.
- Outbox row metadata and payload event metadata match.
- Two concurrent replacements cannot commit the same revision.
- The losing concurrent request creates no outbox row.
- Eligibility rejection, validation failure, and repository failure create no
  event.
- Transaction and outbox insert roll back together.
- Existing reversal still produces `TRANSACTION_REFUNDED`.

### Validation

Use targeted invoice service and PostgreSQL integration tests, concurrency
tests, contract serialization tests, then `mvn test` and
`mvn clean package -DskipTests`. Run the CI-equivalent Maven verification
before merge if the migration or event contract tests are not covered by the
normal suite.

### Acceptance criteria

- One successful replacement maps to one revision, one outbox row, and one
  Kafka event.
- The receiver deployed in PR 02 can consume the event without another stream.
- Concurrent updates cannot duplicate a revision.
- Existing first-invoice and reversal behavior remains valid.

### Deployment and rollback

Deploy only after PR 02 is active and all blocking consumers identified by
PR 01 are compatible. Monitor the first replacement events by event type,
transaction ID, revision, and consumer outcome.

Rollback the application if needed, but retain immutable outbox rows already
created. Do not rewrite their event type or payload.

---

## PR 05 - Make invoice blob replacement failure-safe

**Repository:** `pagopa/idpay-payment`  
**Suggested branch:** `LPBD-safe-invoice-blob-replacement`  
**Base:** `origin/develop`  
**Prerequisites:** PR 04 merged

### Objective

Prevent invoice replacement from deleting the currently committed blob before
the replacement transaction commits.

### Required changes

1. Generate an immutable storage path for each uploaded invoice using the
   transaction ID plus revision or a stable operation/event identifier.
2. Preserve the original filename and document number in canonical invoice
   data without using the original filename as the unique object identity.
3. Change replacement order:
   - validate request and eligibility;
   - upload the new blob;
   - commit transaction and immutable outbox event;
   - delete the previous blob only after commit.
4. Register after-commit cleanup through the repository's transaction
   synchronization convention or a dedicated cleanup service.
5. If the database transaction fails after upload, delete the unreferenced new
   blob or persist a retryable cleanup request. Do not silently leave cleanup
   failures unobservable.
6. A failed old-blob deletion must not roll back or falsify the committed
   replacement. Surface it through retryable cleanup, logs, and metrics.
7. Keep invoice download behavior compatible with the stored path model.
8. Do not mutate an object path referenced by an earlier outbox payload.

### Required tests

- New upload failure leaves transaction, old blob, and outbox unchanged.
- Database failure after upload keeps the old blob and cleans the new blob.
- Successful commit deletes the old blob only after commit.
- Old-blob deletion failure produces a retryable cleanup outcome.
- Two replacements use different blob paths.
- Download resolves the currently committed invoice.
- Event payload references the newly committed invoice data.

### Validation

Use unit tests for transaction synchronization and storage calls plus the
existing storage/WireMock integration convention. Run targeted invoice tests
and the full Maven suite.

### Acceptance criteria

- A committed transaction never points to a blob deleted before commit.
- Previously emitted event payloads continue referring to immutable content.
- Cleanup failures are explicit and retryable.

### Deployment and rollback

This PR changes storage paths additively. Existing invoice paths must remain
readable. Rollback must not delete newly created immutable-path blobs.

---

## PR 06 - Remove obsolete dedicated impact machinery

**Repository:** `pagopa/idpay-transactions`  
**Suggested branch:** `LPBD-remove-dedicated-invoice-impact`  
**Base:** `origin/develop`  
**Prerequisites:** PR 04 merged and unified replacement events verified in development

### Objective

Remove code and contract surfaces that supported a second invoice-impact event
or a second revision watermark.

### Required changes

1. Remove `INVOICE_REPLACED` and `INVOICED_REVERSED` dedicated impact event
   types where they are no longer part of the public contract.
2. Remove the dedicated impact consumer binding, adapter entry point, inbox,
   or transaction-local impact watermark only when repository inspection
   confirms they are used solely by the obsolete dual-event design.
3. Keep or relocate reusable SQL membership transition logic used by
   `TRANSACTION_INVOICE_REPLACED`.
4. Ensure the unified consumer remains the only path that applies replacement
   projection and membership changes.
5. Remove configuration and deployment values for an unused second topic.
6. Update repository documentation and AsyncAPI to reference the unified event
   on `idpay-transaction`.
7. Use a forward-only migration for removable database columns or tables.
   Prefer leaving an unused additive column temporarily if immediate removal
   would make rollback unsafe.

### Required tests

- No production reference remains to the dedicated replacement or reversal
  impact events.
- Unified replacement behavior remains covered for no membership, `CREATED`,
  and move-to-`SUSPENDED`.
- Unified refunded detach remains covered.
- Only one revision watermark participates in unified event ordering.
- Application starts without the removed binding and configuration.

### Validation

Run targeted consumer, SQL adapter, configuration, and integration tests,
followed by the repository's full validation.

### Acceptance criteria

- There is one receiver stream and one event path for invoice replacement.
- No useful transition logic is lost.
- Rollback remains possible without destructive data loss.

### Deployment and rollback

Deploy only after unified events have been observed successfully. Delay
destructive schema cleanup to a later PR if rollback would otherwise require
recreating data.

---

## PR 07 - Finalize operational contract and rollout

**Repository:** `pagopa/idpay-payment`  
**Suggested branch:** `LPBD-finalize-unified-event-rollout`  
**Base:** `origin/develop`  
**Prerequisites:** PR 05 and PR 06 merged

### Objective

Align all payment-side documentation, configuration, examples, and operating
procedures with the deployed unified event design.

### Required changes

1. Update the status and change log in:
   - the source design review;
   - `docs/idpay-payment-reward-batch-impact.md`;
   - this implementation plan; and
   - any superseded implementation plan.
2. Mark obsolete dual-event instructions as superseded rather than leaving
   contradictory active guidance.
3. Finalize `specs/asyncapi.yml` with:
   - the existing transaction topic;
   - `transactionId` key;
   - payload event metadata, including `eventType`;
   - `TRANSACTION_INVOICED`;
   - `TRANSACTION_INVOICE_REPLACED`;
   - `TRANSACTION_REFUNDED`;
   - schema version;
   - revision semantics;
   - complete canonical payload examples.
4. Document dashboards and alerts for:
   - outbox insert rate by event type;
   - duplicate/conflict rate;
   - CDC lag;
   - consumer lag and processing errors;
   - stale revision ignores;
   - replacement membership transitions;
   - blob cleanup backlog and failures.
5. Document replay rules:
   - replay immutable rows without rebuilding payloads;
   - retain the original event ID, type, revision, and key;
   - never replay one revision as both invoiced and replaced;
   - rely on receiver revision idempotency.
6. Document retention ownership and the dedicated role allowed to delete
   archived outbox rows.
7. Render deployment configuration when application or chart values change.

### Required tests and validation

- Validate AsyncAPI using existing repository tooling, if present.
- Run configuration tests when properties or bindings change.
- Render the Helm chart when chart values change.
- Documentation-only changes do not require the full Maven suite unless they
  modify generated or validated contracts.

### Acceptance criteria

- No active document describes the dual-event design as current.
- Operators can detect publication, consumption, revision, and cleanup
  failures.
- Replay and retention preserve immutable event history.
- The progress table records the merged PRs and deployed state.

### Deployment and rollback

Operational defaults must reflect the already verified runtime path. Rollback
is configuration/documentation-only unless this PR changes deployment values.

## Cross-PR verification matrix

The final solution is not complete until the following scenarios have been
demonstrated across the merged PRs:

| Scenario | Expected outcome |
| --- | --- |
| Initial invoice | One `TRANSACTION_INVOICED` event, status `INVOICED` |
| Replacement from `INVOICED` | One `TRANSACTION_INVOICE_REPLACED` event, status `INVOICED`, next revision |
| Replacement from `REWARDED` | One `TRANSACTION_INVOICE_REPLACED` event, status changed to `INVOICED`, next revision |
| Repeated replacement | One new immutable row and revision per operation |
| Concurrent replacement | No duplicate revision; losing operation conflicts or is serialized safely |
| Replacement with no membership | Projection updated, no batch movement |
| Replacement from `CREATED` | Membership retained |
| Replacement from another supported state | Membership moved to outcome month as `SUSPENDED` |
| Reversal | One `TRANSACTION_REFUNDED` event and idempotent detach |
| Duplicate Kafka delivery | Receiver no-op after first commit |
| Stale Kafka delivery | Newer projection and membership remain unchanged |
| Database failure | Transaction and outbox roll back together |
| Blob upload failure | Old committed invoice remains valid |
| Old-blob cleanup failure | Replacement remains committed and cleanup retries |
| Replay | Original event identity, revision, type, key, and payload are retained |

## Final definition of done

- Every published canonical transaction revision has exactly one immutable
  outbox row.
- Invoice replacement uses `TRANSACTION_INVOICE_REPLACED` and status
  `INVOICED`.
- Payload `operationType` is not used as event classification.
- All events use one topic and `transactionId` key.
- `idpay-transactions` updates projection and batch effect in one SQL
  transaction.
- One transaction revision watermark provides ordering and idempotency.
- Reversal uses `TRANSACTION_REFUNDED` without a dedicated secondary impact.
- Concurrent replacements cannot commit the same revision.
- Blob replacement cannot invalidate the currently committed invoice before
  database commit.
- Active documentation and AsyncAPI describe only the unified event design.
