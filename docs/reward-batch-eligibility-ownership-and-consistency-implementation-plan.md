# Reward-batch eligibility ownership implementation plan

## Status

Proposed.

## Objective

Implement the smallest change that establishes the correct ownership and
policy:

1. update `idpay-transactions` so reward-batch eligibility adheres to the
   authoritative matrix;
2. expose that decision through an `idpay-transactions` eligibility endpoint;
3. make `idpay-payment` call the endpoint instead of evaluating reward-batch
   state.

Do not correct or duplicate the policy in `idpay-payment` as an intermediate
step. All work not required for these three outcomes is technical debt and
must be handled separately.

## Ownership

`idpay-payment` owns payment commands and payment-domain validation.

`idpay-transactions` owns:

- reward-batch membership;
- reward-batch lifecycle state;
- transaction state inside a reward batch;
- the reward-batch eligibility decision.

The current payment flow calls:

```http
GET /idpay/transactions/{transactionId}/reward-batch/eligibility?merchantId={merchantId}
```

The response contains raw reward-batch state, which
`RewardBatchEligibilityPreflightServiceImpl` interprets locally. The target
flow must instead return a decision from `idpay-transactions`.

## Scope

### In scope

- implement the authoritative matrix in `idpay-transactions`;
- add exhaustive matrix tests in `idpay-transactions`;
- expose an eligibility decision endpoint from `idpay-transactions`;
- call that endpoint from `idpay-payment` for the two governed operations;
- stop active policy evaluation in `idpay-payment`;
- preserve the existing enablement flag for rollout and rollback.

### Out of scope

Everything else is deferred technical debt, including:

- fixing the policy in `idpay-payment`;
- local/remote comparison or shadow mode;
- projection-lag and missing-projection redesign;
- stronger consistency handling for missing or mismatched membership;
- prepared operations, reservations, locking, or concurrency guarantees;
- `REWARDED` ownership and lifecycle cleanup;
- revision-based synchronization;
- authentication or JWT redesign;
- broad connector error-model changes;
- new reason-code taxonomies beyond what the integration needs;
- payment event, outbox, or blob redesign;
- broad observability, alerting, and runbook changes;
- unrelated cleanup or refactoring;
- legacy endpoint removal unless it is an immediate, contained consequence of
  the migration.

These items must not be added to the implementation PRs.

## Governed operations

The policy applies only to:

| Operation | Payment transition |
| --- | --- |
| Invoice replacement | `INVOICED -> INVOICED` |
| Reversal of an invoiced transaction | `INVOICED -> REFUNDED` |

The policy does not apply to:

- initial invoice creation from `CAPTURED`;
- reversal from `CAPTURED`;
- payment creation, authorization, capture, cancellation, or expiration.

Both governed operations currently use the same matrix. The operation should
still be explicit in the endpoint contract so it can evolve later without
another contract change.

## Caller mapping

Use the existing lifecycle authorities and propagation mechanism:

| Scope | Actor |
| --- | --- |
| `transaction:invoicelifecycle:basic` | Point of sale |
| `transaction:invoicelifecycle:full` | Merchant |

If both authorities are present, merchant/full takes precedence.

Only the minimum plumbing required to preserve this mapping is in scope.
Authentication hardening or redesign is deferred.

## Authoritative eligibility matrix

The following tables are the source of truth for both governed operations.

### Point-of-sale policy

| Batch status | `CONSULTABLE` | `TO_CHECK` | `SUSPENDED` | `APPROVED` | `REJECTED` |
| --- | ---: | ---: | ---: | ---: | ---: |
| `CREATED` | Allow | Deny | Allow | Deny | Deny |
| `SENT` | Deny | Deny | Deny | Deny | Deny |
| `EVALUATING` | Deny | Deny | Deny | Deny | Deny |
| `APPROVING` | Deny | Deny | Deny | Deny | Deny |
| `APPROVED` | Deny | Deny | Deny | Deny | Deny |
| `PENDING_REFUND` | Deny | Deny | Deny | Deny | Deny |
| `REFUNDED` | Deny | Deny | Deny | Deny | Deny |
| `NOT_REFUNDED` | Deny | Deny | Deny | Deny | Deny |

### Merchant policy

| Batch status | `CONSULTABLE` | `TO_CHECK` | `SUSPENDED` | `APPROVED` | `REJECTED` |
| --- | ---: | ---: | ---: | ---: | ---: |
| `CREATED` | Allow | Deny | Allow | Deny | Deny |
| `SENT` | Deny | Deny | Deny | Deny | Deny |
| `EVALUATING` | Allow | Allow | Allow | Deny | Allow |
| `APPROVING` | Deny | Deny | Deny | Deny | Deny |
| `APPROVED` | Deny | Deny | Deny | Deny | Allow |
| `PENDING_REFUND` | Deny | Deny | Deny | Deny | Allow |
| `REFUNDED` | Deny | Deny | Deny | Deny | Allow |
| `NOT_REFUNDED` | Deny | Deny | Deny | Deny | Allow |

`TO_WORK` and `TO_APPROVE` are virtual presentation states and must not be
treated as persisted batch states.

The implementation must evaluate the exact combination:

```text
actor + batchStatus + batchTransactionStatus
```

It must not use independent batch-status and transaction-status allowlists,
because their intersection creates combinations that are not allowed by the
authoritative tables.

## Step 1 - Upgrade the policy in `idpay-transactions`

### Changes

1. Add table-driven tests for every cell in both authoritative matrices.
2. Cover both governed operations with the same fixtures.
3. Implement the matrix in a dedicated transactions-side eligibility service.
4. Reuse the existing transaction, membership, batch, repository, and security
   components.
5. Preserve the existing no-membership behavior.
6. Return an allow/deny domain decision without mutating transaction,
   membership, or batch state.
7. Add the new decision endpoint.

### Endpoint

Use an operation-specific endpoint:

```http
POST /idpay/transactions/{transactionId}/invoice-lifecycle/eligibility
```

Minimal request:

```json
{
  "operation": "INVOICE_REPLACEMENT"
}
```

Supported operations:

```text
INVOICE_REPLACEMENT
INVOICED_REVERSAL
```

Minimal response:

```json
{
  "decision": "ALLOWED"
}
```

or:

```json
{
  "decision": "DENIED"
}
```

The exact request and response may follow existing `idpay-transactions`
contract conventions, but the endpoint must:

- return the eligibility decision;
- not return batch status or in-batch transaction status;
- not require `idpay-payment` to reproduce the policy;
- avoid fields intended only for future consistency or migration designs.

Existing authorization and operational error conventions should be reused
without broad redesign.

### Minimal-change constraints

- Do not modify the policy implementation in `idpay-payment`.
- Do not refactor unrelated transactions flows.
- Do not add a new persistence model.
- Do not add locking, reservation, or finalization logic.
- Do not redesign projection synchronization.
- Change repository code only where required to read the states already used
  by the policy.

### Tests

The transactions-side tests must cover:

- every POS matrix cell;
- every merchant matrix cell;
- both governed operations;
- both authorities, with merchant/full precedence;
- no batch membership;
- unsupported operation;
- unknown state failing closed;
- endpoint serialization and decision mapping.

Cases that require a broader projection or consistency redesign must be
recorded as technical debt rather than added to this step.

### Exit criteria

- `idpay-transactions` matches every cell of the authoritative tables.
- The endpoint returns a decision rather than raw reward-batch state.
- The implementation does not mutate reward-batch data.
- No unrelated changes are included.

## Step 2 - Make `idpay-payment` consume the decision

### Changes

1. Add or adapt the transactions connector for the new endpoint.
2. Call it before side effects for:
   - invoice replacement;
   - reversal from `INVOICED`.
3. Send the operation and reuse the existing authentication propagation.
4. Continue the payment command only for `ALLOWED`.
5. Reject the command for `DENIED`.
6. Fail closed when the eligibility endpoint cannot provide a valid decision.
7. Stop deserializing and evaluating raw reward-batch states in the active
   payment path.
8. Preserve all behavior outside the two governed operations.

Remove the now-unused local policy code only if the deletion is direct and
contained. If removal requires broader changes, leave it unreachable and
track its deletion as technical debt.

### Configuration

Reuse the existing flag:

```yaml
app:
  reward-batch-impact:
    eligibility:
      enabled: false
```

Do not introduce `LOCAL`, `SHADOW_REMOTE`, `REMOTE`, or other migration modes.
When enabled, the transactions decision is authoritative.

### Tests

Add or update only the integration tests needed to prove:

- `ALLOWED` continues invoice replacement;
- `ALLOWED` continues reversal from `INVOICED`;
- `DENIED` stops both operations before side effects;
- invalid or failed endpoint responses fail closed;
- initial invoice creation from `CAPTURED` does not call the endpoint;
- reversal from `CAPTURED` does not call the endpoint;
- payment no longer evaluates reward-batch status combinations.

For a rejected command:

- no old blob is deleted;
- no new blob is uploaded;
- no payment state or document data changes;
- no transaction revision is incremented;
- no success audit or payment outcome event is created.

### Exit criteria

- Both governed payment operations use the transactions-owned decision.
- Payment has no active reward-batch matrix evaluation.
- Operations outside the policy scope are unchanged.
- The existing flag controls rollout and rollback.
- No unrelated refactoring is included.

## Deployment

1. Deploy `idpay-transactions` with the new endpoint.
2. Deploy `idpay-payment` with the new connector and the existing flag
   disabled.
3. Enable the flag in development and test the two governed operations.
4. Promote through the existing environments.
5. Enable the flag in production.

The rollout does not require a shadow phase or a corrected payment fallback.
The exhaustive transactions-side matrix tests are the policy correctness
gate.

## Rollback

Disable the existing eligibility flag.

Do not implement or maintain a corrected local payment policy for rollback.
The additive transactions endpoint may remain deployed.

## Pull request sequence

| PR | Repository | Scope | Dependency |
| --- | --- | --- | --- |
| 1 | `idpay-transactions` | Implement and test the authoritative matrix; expose the decision endpoint | None |
| 2 | `idpay-payment` | Call the new endpoint for the two governed operations and stop active local policy evaluation | PR 1 |

Each PR must remain limited to its row.

## Acceptance scenarios

- POS with `CREATED + CONSULTABLE`: allowed.
- POS with `CREATED + SUSPENDED`: allowed.
- POS with any `EVALUATING` combination: denied.
- Merchant with `EVALUATING + TO_CHECK`: allowed.
- Merchant with `EVALUATING + REJECTED`: allowed.
- Merchant with `EVALUATING + APPROVED`: denied.
- Merchant with `APPROVED + REJECTED`: allowed.
- Merchant with `APPROVED + CONSULTABLE`: denied.
- Any caller with a `SENT` or `APPROVING` batch: denied.
- A transaction with no batch membership preserves the existing allowed
  behavior.
- Invoice replacement and invoiced reversal call the endpoint.
- Initial invoicing and reversal from `CAPTURED` do not call the endpoint.
- Payment performs no side effects after denial or an invalid remote result.

## Deferred technical debt

Track separately:

1. Missing projection versus confirmed no-membership semantics.
2. Inconsistent or dangling membership handling.
3. Concurrency between the eligibility decision and later state changes.
4. Prepared operations, reservations, finalization, and reconciliation.
5. `REWARDED` ownership and lifecycle semantics.
6. Revision-based projection synchronization.
7. Authentication and JWT hardening.
8. Detailed reason codes and connector error categorization.
9. Legacy raw-state endpoint and residual DTO cleanup.
10. Expanded metrics, alerts, and runbooks.
11. Payment event, outbox, blob, and reward-batch lifecycle redesign.

None of these items blocks the two-PR implementation in this plan.

## Target state

After these two PRs:

- `idpay-transactions` is the sole active owner of reward-batch eligibility;
- its policy matches the authoritative matrix;
- `idpay-payment` receives an allow/deny decision from the new endpoint;
- no corrected or duplicated policy is introduced in `idpay-payment`;
- broader consistency and cleanup work remains deferred as technical debt.
