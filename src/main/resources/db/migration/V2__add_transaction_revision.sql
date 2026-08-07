ALTER TABLE "idpay-pagamenti".transaction
    ADD COLUMN IF NOT EXISTS "transactionRevision" BIGINT;

UPDATE "idpay-pagamenti".transaction
SET "transactionRevision" = 0
WHERE "transactionRevision" IS NULL;

ALTER TABLE "idpay-pagamenti".transaction
    ALTER COLUMN "transactionRevision" SET DEFAULT 0,
    ALTER COLUMN "transactionRevision" SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_transaction_transaction_revision_non_negative'
          AND conrelid = '"idpay-pagamenti".transaction'::regclass
    ) THEN
        ALTER TABLE "idpay-pagamenti".transaction
            ADD CONSTRAINT ck_transaction_transaction_revision_non_negative
                CHECK ("transactionRevision" >= 0);
    END IF;
END;
$$;
