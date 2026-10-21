-- Remove legacy refund-batch references from payment transactions schema.
-- These columns are no longer used by payment batch logic.
ALTER TABLE IF EXISTS "idpay-pagamenti".transaction
    DROP COLUMN IF EXISTS "rewardBatchStatusTrx",
    DROP COLUMN IF EXISTS "rewardBatchId";

