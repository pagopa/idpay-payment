

CREATE TABLE transaction (
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


CREATE TABLE transaction_outbox (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL,
    operation_type VARCHAR(10) NOT NULL,
    status VARCHAR(32),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);



CREATE OR REPLACE FUNCTION fn_transaction_outbox()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN

    IF TG_OP = 'INSERT' THEN

        INSERT INTO transaction_outbox (
            transaction_id,
            operation_type,
            status
        )
        VALUES (
            NEW.id,
            'INSERT',
            NEW.status
        );

        RETURN NEW;

    ELSIF TG_OP = 'UPDATE' THEN

        IF OLD.status IS DISTINCT FROM NEW.status THEN

            INSERT INTO transaction_outbox (
                transaction_id,
                operation_type,
                status
            )
            VALUES (
                NEW.id,
                'UPDATE',
                NEW.status
            );

        END IF;

        RETURN NEW;

    ELSIF TG_OP = 'DELETE' THEN

        INSERT INTO transaction_outbox (
            transaction_id,
            operation_type,
            status
        )
        VALUES (
            OLD.id,
            'DELETE',
            OLD.status
        );

        RETURN OLD;

    END IF;

    RETURN NULL;

END;
$$;


CREATE TRIGGER trg_transaction_outbox
AFTER INSERT OR DELETE OR UPDATE OF status
ON transaction
FOR EACH ROW
EXECUTE FUNCTION fn_transaction_outbox();

