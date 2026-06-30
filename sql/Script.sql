CREATE SCHEMA IF NOT EXISTS idpay;

CREATE TABLE IF NOT EXISTS idpay.reward_batch (
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

CREATE TABLE IF NOT EXISTS idpay.transaction (
    id VARCHAR(64) PRIMARY KEY,
    trx_code VARCHAR(64) NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    operation_type_transcoded VARCHAR(32),
    status VARCHAR(32) NOT NULL,
    trx_date TIMESTAMPTZ NOT NULL,
    trx_charge_date TIMESTAMPTZ,
    trx_end_date TIMESTAMPTZ,
    elaboration_date TIMESTAMP,
    update_date TIMESTAMP,
    user_id VARCHAR(64),
    merchant_id VARCHAR(64),
    acquirer_id VARCHAR(64),
    point_of_sale_id VARCHAR(64),
    amount_cents BIGINT,
    effective_amount_cents BIGINT,
    voucher_amount_cents BIGINT,
    amount_currency VARCHAR(8),
    channel VARCHAR(32),
    initiative_id VARCHAR(64),
    initiative_name VARCHAR(255),
    initiatives JSONB,
    business_name VARCHAR(255),
    franchise_name VARCHAR(255),
    invoice_filename VARCHAR(255),
    invoice_doc_number VARCHAR(255),
    credit_note_filename VARCHAR(255),
    credit_note_doc_number VARCHAR(255),
    correlation_id VARCHAR(128),
    created_at TIMESTAMP,
    reward_batch_status_trx VARCHAR(64),
    reward_batch_id VARCHAR(64),
    product_gtin VARCHAR(64),
    product_name VARCHAR(64),
    id_trx_acquirer VARCHAR(64),
    merchant_fiscal_code VARCHAR(64),
    vat VARCHAR(32),
    point_of_sale_type VARCHAR(32),
    family_id VARCHAR(64),
    reward_cents BIGINT,
    counter_version BIGINT,
    rewards JSONB,
    rejection_reasons JSONB,
    initiative_rejection_reasons JSONB,
    additional_properties JSONB,
    mcc VARCHAR(32),
    id_trx_issuer VARCHAR(32),
    extended_authorization BOOLEAN
);

CREATE TABLE IF NOT EXISTS idpay.transaction_outbox (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    published BOOLEAN,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_transaction_outbox'
    ) THEN
        ALTER TABLE idpay.transaction_outbox
            ADD CONSTRAINT uk_transaction_outbox
            UNIQUE (transaction_id, event_type);
    END IF;
END $$;

CREATE OR REPLACE FUNCTION idpay.fn_transaction_outbox()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO idpay.transaction_outbox (
        transaction_id,
        event_type,
        payload,
		published
    )
    VALUES (
        NEW.id,
        'TRANSACTION_' || NEW.status,
        to_jsonb(NEW),
		false
    )
    ON CONFLICT (transaction_id, event_type)
    DO NOTHING;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_transaction_outbox
ON idpay.transaction;

CREATE TRIGGER trg_transaction_outbox
AFTER INSERT OR UPDATE OF status
ON idpay.transaction
FOR EACH ROW
EXECUTE FUNCTION idpay.fn_transaction_outbox();