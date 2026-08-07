# `idpay-payment` reward-batch impact implementation plan

**Source specification:** [idpay-payment-reward-batch-impact.md](./idpay-payment-reward-batch-impact.md)

This document is the execution plan for the payment-side implementation. It is
structured as independently deployable iterations so that every intermediate
version can be released to the dev environment without requiring the final
reward-batch flow to be enabled.

## 1. Non-negotiable invariants

Every iteration and every PR must preserve these rules:

- Payment remains the owner of invoice/reversal commands, blob operations, and
  the authoritative transaction state.
- Payment never writes reward-batch membership or in-batch evaluation fields.
- `transactionRevision` is a new lifecycle revision. It must never reuse
  `counterVersion`, which belongs to the reward-calculator ETag flow.
- A new transaction starts at revision `0`. A published canonical change
  increments the revision exactly once.
- The envelope revision and the embedded transaction projection use the same
  positive value.
- Invoice replacement and invoiced reversal persist the transaction update and
  their dedicated outbox record in one PostgreSQL transaction.
- Dedicated impact publication happens only after the payment transaction
  commits. A send retry reuses the exact same event ID and serialized payload.
- Generic snapshots and dedicated impacts are independently idempotent and may
  arrive in either order.
- Existing invoice/reversal HTTP behavior, blob paths, audit logging, generic
  transaction notifications, and payment validation remain unchanged while the
  new flags are disabled.
- No destructive migration, route removal, or feature enablement is allowed
  until the receiving `idpay-transactions` deployment is compatible.

## 2. Repository reality and implementation anchors

The source specification mentions `TransactionInProgress` and
`RewardTransactionDTO`; this repository currently implements the authoritative
record as the JPA `Transaction` entity and currently publishes that entity
directly for generic transaction notifications. The implementation must use
the actual payment-side seams below and introduce a wire DTO at the event
boundary instead of exposing the JPA entity.

| Concern | Current anchor | Required treatment |
| --- | --- | --- |
| Authoritative transaction | `src/main/java/it/gov/pagopa/payment/entity/Transaction.java` | Add a distinct `transactionRevision`; keep reward-calculator `counterVersion` unchanged. |
| Creation | `dto/mapper/TransactionMapper.java`, barcode creation services | Initialize revision `0` for every creation path, including reset/extended transactions. |
| Repository writes | `repository/TransactionRepository.java`, `TransactionRepositoryExt.java` | Audit every update query and increment only when the corresponding canonical snapshot is published. |
| Invoice command | `service/payment/common/CommonInvoiceServiceImpl.java` | Add eligibility preflight and transactional revision/outbox handling without changing validation or blob behavior. |
| Reversal command | `service/payment/common/CommonReversalServiceImpl.java` | Add eligibility preflight and transactional revision/outbox handling without changing validation or blob behavior. |
| Generic event producer | `connector/event/trx/TransactionNotifierServiceImpl.java` | Keep the existing binding; publish a contract DTO rather than the JPA entity after the DTO contract is agreed. |
| Existing stream configuration | `src/main/resources/application.yml` | Add new producer configuration only as an additive, disabled-by-default binding. |
| Existing contract | `specs/asyncapi.yml` | Add revision and the dedicated impact channel/message without deleting existing channels prematurely. |
| Database delivery | `src/main/resources/db/migration/` | Flyway creates the schema and applies forward-only migrations automatically. Existing DEV and UAT databases baseline at version `1`; fresh databases run `V1`. |
| Existing outbox trigger | `src/main/resources/db/migration/V1__initial_payment_schema.sql`, `transaction_outbox` | Do not repurpose it for impacts: its status-based identity and `(transaction_id,event_type)` uniqueness cannot represent repeated invoice replacements safely, and its raw entity payload can leak local fields. |

## 3. Decisions required before implementation

These values are cross-service contract decisions, not implementation guesses.
They must be recorded in the PR or linked integration decision before
Iteration 3:

1. The exact `findEligibility(merchantId, transactionId)` HTTP method, path,
   authentication/forwarded headers, timeout, and response for "no local
   membership".
2. The eligibility policy:
   - recommended: a successful response, including no membership, preserves the
     current payment command;
   - a documented transport/5xx failure blocks the command when the eligibility
     flag is enabled, before blob or transaction mutation;
   - no broad fallback or silent default is allowed.
3. The dedicated Kafka topic, binder, binding name, consumer group/auto-start
   policy, and environment variable names.
4. The exact payment-owned fields in the shared `RewardTransactionDTO`, the
   Jackson null/unknown-field policy, and the compatibility behavior of the
   existing generic snapshot consumer.
Until the remaining decisions are closed, an agent must not invent an endpoint or
enable a remote call in a payment command.

## 4. Rules for coding-agent handoff

Each iteration is one independently reviewable PR or one clearly isolated
commit series. The agent must:

- modify only the surfaces listed for the iteration;
- add or update unit tests in the same PR as production code;
- preserve the default-off behavior of all new integration flags;
- run the targeted tests, `mvn test`, and `mvn clean package -DskipTests`;
- run the repository CI Maven verification before the final integration PR;
- include a deployment note stating the expected previous and next service
  versions;
- update the source specification status/checklist only when the iteration is
  actually deployed and verified.

The dependency order is:

```text
I1 revision storage
  -> I2 revision-aware generic snapshot
  -> I3 eligibility client and guarded preflight
  -> I4 impact contract and transactional outbox
  -> I5 post-commit publisher and Kafka binding
  -> I6 cross-service dev canary and enablement
  -> I7 hardening and removal of temporary compatibility code
```

## 5. Progressive implementation iterations

### Iteration 1 - Additive revision storage and initialization

**Deployable outcome:** the payment application stores a lifecycle revision,
but all existing runtime behavior and event payloads remain unchanged.

**Implementation tasks**

1. Add a forward-only database migration for
   `"transactionRevision" BIGINT NOT NULL DEFAULT 0`.
   Backfill existing rows to `0` and add a non-negative check if the migration
   mechanism supports it. The migration must be safe when applied once and
   safe to inspect/retry without destructive statements.
2. Add `transactionRevision` to `Transaction` with the repository's existing
   quoted-column conventions.
3. Initialize `0L` in every transaction creation path:
   `TransactionMapper.transactionCreationRequestToTransaction`,
   `transactionBarCodeCreationRequestToTransaction`, and any reset/extended
   transaction factory.
4. Add a small revision invariant/helper only if it removes duplication. Do not
   change `counterVersion`.
5. Update test fakers and fixtures so newly created transactions have an
   explicit revision.

**Tests**

- Entity/mapper tests verify every creation path starts at `0`.
- Tests verify `counterVersion` and `transactionRevision` are independent.
- Migration/schema verification covers existing rows, new rows, null input, and
  negative values.
- Existing payment service tests remain green without enabling any new flag.

**Dev gate and rollback**

- Deploy against the previous service version and confirm old binaries ignore
  the additive column.
- Keep all new integration flags absent or `false`.
- Rollback is application-only; retain the additive column and default.

### Iteration 2 - Revision-aware generic transaction snapshots

**Deployable outcome:** every payment-produced generic transaction snapshot can
carry `transactionRevision`, while old consumers remain compatible with the
additive field.

**Implementation tasks**

1. Freeze and implement the payment-side `RewardTransactionDTO` wire model in
   the agreed event package. It must contain payment-owned canonical fields
   only and must not expose `rewardBatchId`, in-batch status/reason, inclusion
   date, last elaborated month, sampling key, checks error, or other local
   reward-batch fields.
2. Add a mapper from `Transaction` to the wire DTO. Enforce the event
   invariants:
   - exactly one initiative;
   - authoritative `merchantId` from the transaction;
   - no accidental use of `counterVersion` as the revision;
   - stable field names and null handling.
3. Change `TransactionNotifierService` and its implementation to build the
   generic message from the DTO, preserving the existing topic, key behavior,
   and payment-owned fields already consumed by downstream services.
4. Inventory all canonical write paths and update their revision handling. At
   minimum review:
   `CommonPreAuthServiceImpl`, `CommonAuthServiceImpl`,
   `BarCodeCaptureServiceImpl`, `CommonConfirmServiceImpl`,
   `CommonCancelServiceImpl`, timeout/expiration services,
   `TransactionRepositoryExt`, and the invoice/reversal paths.
5. For direct JPQL/native update queries, use a database-side increment or a
   row-locked update so concurrent writers cannot overwrite a revision. Do not
   rely on a read-modify-write increment in Java.
6. Update `specs/asyncapi.yml` with the additive revision field and the final
   generic DTO schema. Do not remove a legacy field until the receiving
   contract test proves it is unused.

**Tests**

- Mapper/serialization tests assert the exact JSON shape and absence of all
  reward-batch-local fields.
- `TransactionNotifierServiceTest` asserts the DTO payload, key, and existing
  binding are unchanged apart from the additive revision.
- Repository/update tests assert one increment per published canonical change
  and no increment for bookkeeping updates that are not published.
- Tests cover revision `0`, positive revisions, stale/null legacy values, and
  an attempted multi-initiative projection.
- Existing confirm/cancel/expiration tests verify their observable behavior and
  notification error paths remain unchanged.

**Dev gate and rollback**

- Deploy with the impact and eligibility flags disabled.
- Verify the existing `idpay-transaction` stream still receives its prior
  event types and downstream consumers accept the added field.
- If the DTO compatibility check fails, keep the old notifier behind a
  configuration switch; do not roll back the database migration.

### Iteration 3 - Eligibility connector and guarded command preflight

**Deployable outcome:** payment contains the read-only eligibility client, but
the invoice/reversal commands call it only when explicitly enabled.

**Implementation tasks**

1. Add an interface/implementation pair following the existing connector
   pattern, for example:
   `connector/rest/rewardbatch/RewardBatchConnector.java` and
   `RewardBatchConnectorImpl.java`.
2. Add the agreed Feign client, response DTO, and configuration property for
   the `idpay-transactions` base URL. Keep the response limited to:
   `transactionId`, `initiativeId`, `merchantId`, `rewardBatchId`,
   `transactionStatus`, `batchStatus`, and `batchTransactionStatus`.
3. Map only documented "no membership" responses to an empty result. Surface
   all other client, timeout, authentication, and server errors through the
   repository's normal exception handling.
4. Add `eligibility.enabled=false` under a dedicated
   `app.reward-batch-impact` configuration section.
5. When enabled, call `findEligibility(merchantId, transactionId)` after
   payment validation has loaded the authoritative transaction and before
   invoice/reversal state or blob mutation. Revalidate the transaction inside
   the eventual database transaction to protect against races.
6. Keep eligibility data out of all generic and impact event payloads. It is
   only an input to the agreed payment policy.

**Tests**

- Connector unit/WireMock tests cover the agreed success, no-membership,
  validation-error, timeout, and server-error responses.
- `CommonInvoiceServiceImplTest` and `CommonReversalServiceImplTest` verify:
  call order, disabled-flag behavior, successful preflight, and no blob/save
  when a required preflight error is returned.
- Tests verify that eligibility fields never appear in an event payload.
- Configuration tests verify the safe default is disabled.

**Dev gate and rollback**

- Deploy the payment version with the flag disabled while the endpoint is
  deployed or stubbed.
- Enable the flag only in dev after the endpoint contract is available.
- Disable the flag to restore the previous command path; do not add a local
  fallback query or write coupling.

### Iteration 4 - Dedicated impact contract and atomic transactional outbox

**Deployable outcome:** successful invoice replacement and invoiced reversal
create durable, disabled-by-default impact records in the same database
transaction as the authoritative payment update. No Kafka impact is sent yet.

**Implementation tasks**

1. Add the event model and enum:
   `eventId`, `schemaVersion=1`, `impactType`,
   `occurredAt`, `transactionRevision`, and the canonical
   `RewardTransactionDTO`.
2. Add a dedicated outbox table/entity/repository. It must support repeated
   invoice replacements and stable retries. At minimum it needs:
   `event_id`, `transaction_id`, `transaction_revision`, `impact_type`,
   `occurred_at`, serialized `payload`, creation time, attempt/retry data,
   lease data, and published time/status.
3. Enforce unique event identity and
   `(transaction_id, transaction_revision)`. Do not use the existing
   `(transaction_id,event_type)` legacy constraint.
4. Add `outbox.enabled=false`. When enabled, persist only the two specified
   impact types:

   | Payment outcome | Impact | Projection status |
   | --- | --- | --- |
   | Successful invoice replacement | `INVOICE_REPLACED` | `INVOICED` |
   | Successful reversal of an invoiced transaction | `INVOICED_REVERSED` | `REFUNDED` |

5. Refactor each command into a safe sequence:
   - validate the request and load the transaction;
   - perform the guarded eligibility preflight;
   - perform the existing blob operation and any required POS enrichment;
   - enter a transactional application method that locks/revalidates the
     transaction, increments the revision atomically, applies the outcome,
     builds the canonical DTO, and inserts the outbox row;
   - commit before any Kafka send.
6. Use one `occurredAt` value for the persisted outcome and event. Store the
   event's serialized payload at outbox creation time; retries must not rebuild
   it from a later transaction state.
7. For `INVOICE_REPLACED`, guarantee `pointOfSaleType` is present in the
   projection. Legacy rows missing it must be enriched before the commit or
   the operation must fail without changing the transaction when the impact
   flow is enabled.
8. Generate a new event ID for each successful operation. Replacing an invoice
   twice produces two positive revisions and two distinct outbox records.
9. Keep the existing `transaction_outbox` trigger and generic notifications
   separate from the dedicated impact outbox.

**Tests**

- Event builder tests assert exact envelope values, matching envelope/embedded
  revisions, positive revisions, one initiative, authoritative merchant ID,
  post-operation status, and absent local reward-batch fields.
- Invoice tests cover first invoice, invoice replacement, missing POS details,
  repeated replacement, upload failure, eligibility failure, transaction
  conflict, and repository failure.
- Reversal tests cover captured reversal, invoiced reversal, invalid status,
  upload failure, eligibility failure, and repository failure.
- Outbox repository/service tests cover uniqueness, stable payloads, failed
  transaction rollback, repeated invoice replacement, and no record for
  unsuccessful commands.
- Add a transactional integration test for the custom update/outbox write if
  the repository's existing database test setup permits it; otherwise keep the
  SQL migration verification as a release gate and do not claim atomicity from
  Mockito tests alone.

**Dev gate and rollback**

- Deploy with eligibility and outbox flags disabled first.
- Enable outbox persistence with the publisher disabled and verify rows are
  created only after successful commands.
- Disable the outbox flag to return to the prior command behavior; retain
  already-created rows for later controlled handling.

### Iteration 5 - Post-commit publisher and dedicated Kafka binding

**Deployable outcome:** the payment service can publish durable impact records
at least once, but the publisher remains disabled until the receiving binding
is deployed.

**Implementation tasks**

1. Add a dedicated publisher service, separate from
   `TransactionNotifierService`, using the agreed topic/binder and the
   repository's Spring Cloud Stream conventions.
2. Add the producer binding and environment variables to
   `src/main/resources/application.yml`. Add the event channel and schemas to
   `specs/asyncapi.yml`. All binding additions must be additive.
3. Add `publisher.enabled=false` and polling/backoff/lease configuration.
4. Claim pending rows with row-level locking/`SKIP LOCKED` or the equivalent
   safe mechanism. Never delete a row before a successful send.
5. Send the stored serialized payload with transaction ID as the Kafka key.
   Mark the row published only after a successful send. A `false` result,
   exception, process restart, or crash after send must leave a retryable row;
   a duplicate retry must have the same event ID, revision, and payload.
6. Add bounded retry/backoff and actionable logs/metrics. Sanitize transaction
   identifiers in logs and do not use the generic error notifier as a
   substitute for durable outbox retry.
7. Keep the producer idle when disabled so old deployments do not require the
   new receiver or topic.

**Tests**

- Publisher unit tests cover claim ordering, key/header construction, exact
  payload preservation, successful acknowledgement, `send=false`, send
  exception, retry/backoff, lease recovery, and duplicate delivery.
- Binding/configuration tests verify the new destination and safe defaults.
- Contract tests validate the emitted JSON against the AsyncAPI example and
  assert all forbidden reward-batch-local fields are absent.
- Existing `TransactionNotifierServiceTest` remains green for the generic
  topic.

**Dev gate and rollback**

- Deploy the producer with `publisher.enabled=false`.
- Verify application startup/health while the receiver binding is absent.
- Enable the publisher only after the receiver has deployed its compatible
  consumer and topic configuration.
- To roll back, disable the publisher and preserve pending rows; do not
  republish from a newly rebuilt live transaction.

### Iteration 6 - Cross-service dev canary and controlled enablement

**Prerequisite:** `idpay-transactions` has deployed the compatible revision
projection, eligibility endpoint, impact validation/handler, and production
consumer binding. The receiver must not require payment to write reward-batch
data.

**Deployment sequence**

1. Deploy both services with eligibility, outbox, publisher, and receiver
   consumer enablement disabled.
2. Deploy the payment revision/DTO version and verify generic snapshots in the
   existing topic.
3. Enable payment eligibility only. Test a transaction with membership, a
   transaction without membership, and a documented connector failure.
4. Enable payment outbox persistence with publisher disabled. Execute one
   invoice replacement and one invoiced reversal; verify exactly one durable
   record per successful operation and no record for failed operations.
5. Enable the receiver consumer with its normal retry/validation behavior.
6. Enable the payment publisher for a small dev canary.
7. Verify both delivery orders:
   - generic snapshot then impact;
   - impact then generic snapshot.
8. Force a publisher retry and a process restart. Verify the same event ID and
   payload are retried and the receiver applies each revision once.
9. Verify the receiver effects:
   - invoice replacement from `CREATED` keeps membership/in-batch state;
   - invoice replacement from another source moves membership and suspends the
     target assignment;
   - invoiced reversal detaches membership and clears local assignment data.
10. Verify an `INVOICED` generic snapshot never deletes/cancels the payment
    transaction and a stale generic snapshot cannot overwrite a newer impact.

**Exit criteria**

- All contract, ordering, retry, idempotency, and error-path checks pass.
- No increase in payment command failures, blob errors, generic event errors, or
  outbox backlog beyond the agreed threshold.
- The source specification status table and checklist are updated with the
  deployed versions and flag state.

### Iteration 7 - Hardening and completion

Only after the dev canary is stable:

1. Keep the feature flags available for rollback, but set the agreed dev
   defaults explicitly in deployment values.
2. Remove only temporary compatibility branches that are proven unnecessary;
   do not remove the revision column, dedicated outbox, generic DTO, or stable
   event identity.
3. Document operational dashboards/alerts for pending, retrying, and
   permanently failing outbox rows.
4. Confirm the direct SQL cutover in `idpay-transactions` no longer relies on
   local payment-owned invoice/reversal routes before any legacy route cleanup.
5. Update `docs/idpay-payment-reward-batch-impact.md` status/checklist and
   changelog in the same PR as the final enablement.

## 6. Required validation for every iteration

The coding agent must run the narrowest relevant tests first, then the full
repository validation:

```bash
mvn -Dtest=<changed-test-classes> test
mvn test
mvn clean package -DskipTests
```

Before final integration enablement, run the CI-equivalent Maven verification:

```bash
mvn clean org.jacoco:jacoco-maven-plugin:0.8.14:prepare-agent verify \
  org.jacoco:jacoco-maven-plugin:0.8.14:report \
  org.jacoco:jacoco-maven-plugin:0.8.14:report-aggregate -B
```

If the Helm chart is part of the iteration, also render it with:

```bash
helm dep build && helm template . -f values-dev.yaml --debug
```

No iteration is complete until the targeted tests, full tests, build, and
required deployment/configuration checks pass.

## 7. Final definition of done

- `transactionRevision` is initialized, persisted, and atomically advanced
  without touching `counterVersion`.
- Generic snapshots include the revision and exclude reward-batch-local state.
- Eligibility is read-only, guarded, explicitly configured, and never copied
  into an impact event.
- Invoice replacement and invoiced reversal create exact, stable impact
  envelopes in a durable post-commit outbox.
- The publisher is at-least-once, retry-safe, keyed by transaction ID, and
  disabled until the receiver is compatible.
- All changed behavior has unit tests plus the necessary persistence/contract
  verification.
- Existing payment behavior remains available through a flag-off rollback.
- The dev deployment has exercised ordering, retries, duplicate delivery,
  failure paths, and the receiver-side local effects.
