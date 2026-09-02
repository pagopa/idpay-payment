-- Temporarily disable the immutability guard so existing rows can be backfilled.
-- IF EXISTS keeps the migration safe when the script is executed again.
DROP TRIGGER IF EXISTS trg_transaction_outbox_reject_update
    ON "idpay-pagamenti".transaction_outbox;

-- Add the relational metadata required by the immutable event contract.
ALTER TABLE "idpay-pagamenti".transaction_outbox
    ADD COLUMN IF NOT EXISTS transaction_revision BIGINT,
    ADD COLUMN IF NOT EXISTS schema_version INTEGER,
    ADD COLUMN IF NOT EXISTS occurred_at TIMESTAMPTZ;

-- Backfill only missing values, preserving metadata already populated by a
-- previous execution. Legacy payloads without a revision use revision 0.
UPDATE "idpay-pagamenti".transaction_outbox
SET transaction_revision = COALESCE(transaction_revision, CASE
        WHEN jsonb_typeof(payload -> 'transactionRevision') = 'number'
            THEN (payload ->> 'transactionRevision')::BIGINT
        WHEN jsonb_typeof(payload -> 'transactionRevision') = 'string'
             AND payload ->> 'transactionRevision' ~ '^[0-9]+$'
            THEN (payload ->> 'transactionRevision')::BIGINT
        ELSE 0
    END),
    schema_version = COALESCE(schema_version, 1),
    occurred_at = COALESCE(occurred_at, CASE
        WHEN jsonb_typeof(payload -> 'occurredAt') = 'string'
            THEN (payload ->> 'occurredAt')::TIMESTAMPTZ
        ELSE created_at
    END);

-- Every immutable event must have complete relational metadata.
ALTER TABLE "idpay-pagamenti".transaction_outbox
    ALTER COLUMN transaction_revision SET NOT NULL,
    ALTER COLUMN schema_version SET DEFAULT 1,
    ALTER COLUMN schema_version SET NOT NULL,
    ALTER COLUMN occurred_at SET NOT NULL;

-- Flatten event metadata into the stored payload. Values on the right side of
-- the JSONB concatenation replace inconsistent legacy payload values.
UPDATE "idpay-pagamenti".transaction_outbox
SET payload = payload || jsonb_build_object(
        'eventId', id::TEXT,
        'schemaVersion', schema_version,
        'eventType', event_type,
        'occurredAt', occurred_at,
        'transactionRevision', transaction_revision
    );

-- Replace the mutable event-type identity with transaction revision identity.
ALTER TABLE "idpay-pagamenti".transaction_outbox
    DROP CONSTRAINT IF EXISTS uk_transaction_outbox;

-- PostgreSQL does not support ADD CONSTRAINT IF NOT EXISTS, so inspect the
-- catalog before creating the revision-keyed uniqueness constraint.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_transaction_outbox_transaction_revision'
          AND conrelid = '"idpay-pagamenti".transaction_outbox'::regclass
    ) THEN
        ALTER TABLE "idpay-pagamenti".transaction_outbox
            ADD CONSTRAINT uk_transaction_outbox_transaction_revision
                UNIQUE (transaction_id, transaction_revision);
    END IF;
END;
$$;

-- Build and insert one immutable event from the committed transaction snapshot.
-- Existing classification remains TRANSACTION_<status> until the next step.
CREATE OR REPLACE FUNCTION "idpay-pagamenti".fn_transaction_outbox()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    outbox_event_id BIGINT;
    outbox_event_type VARCHAR(64);
    outbox_occurred_at TIMESTAMPTZ;
    outbox_payload JSONB;
BEGIN
    -- Reserve the identity first because the same stable ID is stored in the
    -- relational row and in payload.eventId.
    outbox_event_id := nextval(
        pg_get_serial_sequence('"idpay-pagamenti".transaction_outbox', 'id')
    );
    outbox_event_type := 'TRANSACTION_' || NEW.status;
    outbox_occurred_at := clock_timestamp();
    outbox_payload := jsonb_strip_nulls(to_jsonb(NEW)) || jsonb_build_object(
        'eventId', outbox_event_id::TEXT,
        'schemaVersion', 1,
        'eventType', outbox_event_type,
        'occurredAt', outbox_occurred_at,
        'transactionRevision', NEW."transactionRevision"
    );

    -- Explicit identity insertion is required after reserving the event ID.
    -- An exact duplicate revision is ignored and never mutates the existing row.
    INSERT INTO "idpay-pagamenti".transaction_outbox (
        id,
        transaction_id,
        user_id,
        event_type,
        payload,
        created_at,
        transaction_revision,
        schema_version,
        occurred_at
    )
    OVERRIDING SYSTEM VALUE
    VALUES (
        outbox_event_id,
        NEW.id,
        NEW."userId",
        outbox_event_type,
        outbox_payload,
        outbox_occurred_at,
        NEW."transactionRevision",
        1,
        outbox_occurred_at
    )
    ON CONFLICT (transaction_id, transaction_revision) DO NOTHING;

    RETURN NEW;
END;
$$;

-- Reject every update to an event row after the migration has completed.
CREATE OR REPLACE FUNCTION "idpay-pagamenti".fn_reject_transaction_outbox_update()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'transaction_outbox rows are immutable'
        USING ERRCODE = '55000';
END;
$$;

COMMENT ON FUNCTION "idpay-pagamenti".fn_reject_transaction_outbox_update()
IS 'Rejects event mutation. Retention must use DELETE through a dedicated maintenance role.';

-- Re-enable the immutability guard removed before the backfill.
CREATE TRIGGER trg_transaction_outbox_reject_update
BEFORE UPDATE ON "idpay-pagamenti".transaction_outbox
FOR EACH ROW
EXECUTE FUNCTION "idpay-pagamenti".fn_reject_transaction_outbox_update();
