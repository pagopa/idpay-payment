CREATE DATABASE "idpay-database";

BEGIN;

CREATE SCHEMA IF NOT EXISTS "idpay-pagamenti";

CREATE TABLE IF NOT EXISTS "idpay-pagamenti".reward_batch (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL,
    initiative_id VARCHAR(64),
    business_name VARCHAR(255),
    month VARCHAR(7) NOT NULL,
    pos_type VARCHAR(32),
    status VARCHAR(32),
    partial BOOLEAN,
    approved_amount_cents BIGINT DEFAULT 0,
    suspended_amount_cents BIGINT DEFAULT 0,
    rejected_amount_cents BIGINT DEFAULT 0,
    number_of_transactions BIGINT DEFAULT 0,
    number_of_transactions_elaborated BIGINT DEFAULT 0,
    number_of_transactions_suspended BIGINT DEFAULT 0,
    number_of_transactions_rejected BIGINT DEFAULT 0,
    refund_valuta_date DATE,
    refund_error_message TEXT,
    refund_outcome_timestamp TIMESTAMP,
    report_path VARCHAR(512),
    filename VARCHAR(255),
    creation_date TIMESTAMP,
    update_date TIMESTAMP,
    approval_date TIMESTAMP,
    merchant_send_date TIMESTAMP
);

CREATE TABLE IF NOT EXISTS "idpay-pagamenti".transaction (
    id VARCHAR(64) PRIMARY KEY,
    "trxCode" VARCHAR(64) NOT NULL,
    "operationType" VARCHAR(32) NOT NULL,
    "operationTypeTranscoded" VARCHAR(32),
    status VARCHAR(32) NOT NULL,
    "trxDate" TIMESTAMPTZ NOT NULL,
    "trxChargeDate" TIMESTAMPTZ,
    "trxEndDate" TIMESTAMPTZ,
    "elaborationDateTime" TIMESTAMP,
    "updateDate" TIMESTAMP,
    "userId" VARCHAR(64),
    "merchantId" VARCHAR(64),
    "acquirerId" VARCHAR(64),
    "pointOfSaleId" VARCHAR(64),
    "amountCents" BIGINT,
    "effectiveAmountCents" BIGINT,
    "voucherAmountCents" BIGINT,
    "amountCurrency" VARCHAR(8),
    channel VARCHAR(32),
    "initiativeId" VARCHAR(64),
    "initiativeName" VARCHAR(255),
    initiatives JSONB,
    "businessName" VARCHAR(255),
    "franchiseName" VARCHAR(255),
    "invoiceData" JSONB,
    "creditNoteData" JSONB,
    "correlationId" VARCHAR(128),
    "createdAt" TIMESTAMP,
    "rewardBatchStatusTrx" VARCHAR(64),
    "rewardBatchId" VARCHAR(64),
    "idTrxAcquirer" VARCHAR(64),
    "merchantFiscalCode" VARCHAR(64),
    vat VARCHAR(32),
    "pointOfSaleType" VARCHAR(32),
    "familyId" VARCHAR(64),
    "rewardCents" BIGINT,
    "counterVersion" BIGINT,
    rewards JSONB,
    "rejectionReasons" JSONB,
    "initiativeRejectionReasons" JSONB,
    "additionalProperties" JSONB,
    mcc VARCHAR(32),
    "idTrxIssuer" VARCHAR(32),
    "extendedAuthorization" BOOLEAN
);

CREATE TABLE IF NOT EXISTS "idpay-pagamenti".transaction_outbox (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_transaction_outbox UNIQUE (transaction_id, event_type)
);

CREATE OR REPLACE FUNCTION "idpay-pagamenti".fn_transaction_outbox()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO "idpay-pagamenti".transaction_outbox (
        transaction_id,
        event_type,
        payload
    )
    VALUES (
        NEW.id,
        'TRANSACTION_' || NEW.status,
        jsonb_strip_nulls(to_jsonb(NEW))
    )
    ON CONFLICT (transaction_id, event_type)
    DO UPDATE SET
        payload = EXCLUDED.payload,
        created_at = now();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_transaction_outbox
ON "idpay-pagamenti".transaction;

CREATE TRIGGER trg_transaction_outbox
AFTER INSERT OR UPDATE OF status
ON "idpay-pagamenti".transaction
FOR EACH ROW
EXECUTE FUNCTION "idpay-pagamenti".fn_transaction_outbox();

COMMIT;