-- Existing deployments may still have the legacy timezone-less created_at
-- column because CREATE TABLE IF NOT EXISTS does not alter an existing table.
-- Payload occurredAt is the authoritative instant and avoids interpreting
-- Java-written UTC wall-clock values as Europe/Rome local time.
DROP TRIGGER IF EXISTS trg_transaction_outbox_reject_update
    ON "idpay-pagamenti".transaction_outbox;

ALTER TABLE "idpay-pagamenti".transaction_outbox
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE TIMESTAMPTZ
        USING (payload ->> 'occurredAt')::TIMESTAMPTZ,
    ALTER COLUMN created_at SET DEFAULT now();

UPDATE "idpay-pagamenti".transaction_outbox
SET occurred_at = (payload ->> 'occurredAt')::TIMESTAMPTZ
WHERE occurred_at IS DISTINCT FROM (payload ->> 'occurredAt')::TIMESTAMPTZ;

-- Serialize future event occurrence timestamps in UTC regardless of the
-- database session timezone.
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
    outbox_event_id := nextval(
        pg_get_serial_sequence('"idpay-pagamenti".transaction_outbox', 'id')
    );
    outbox_event_type := 'TRANSACTION_' || NEW.status;
    outbox_occurred_at := clock_timestamp();
    outbox_payload := jsonb_strip_nulls(to_jsonb(NEW)) || jsonb_build_object(
        'eventId', outbox_event_id::TEXT,
        'schemaVersion', 1,
        'eventType', outbox_event_type,
        'occurredAt', to_char(
            outbox_occurred_at AT TIME ZONE 'UTC',
            'YYYY-MM-DD"T"HH24:MI:SS.US"Z"'
        ),
        'transactionRevision', NEW."transactionRevision"
    );

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

CREATE TRIGGER trg_transaction_outbox_reject_update
BEFORE UPDATE ON "idpay-pagamenti".transaction_outbox
FOR EACH ROW
EXECUTE FUNCTION "idpay-pagamenti".fn_reject_transaction_outbox_update();
