-- Invoice commands insert their typed outbox event through the Java
-- transactional persistence adapter. The generic trigger keeps handling all
-- other status transitions, including REFUNDED.
DROP TRIGGER IF EXISTS trg_transaction_outbox_update
    ON "idpay-pagamenti".transaction;

CREATE TRIGGER trg_transaction_outbox_update
AFTER UPDATE ON "idpay-pagamenti".transaction
FOR EACH ROW
WHEN (
    OLD.status IS DISTINCT FROM NEW.status
    AND NEW.status <> 'INVOICED'
)
EXECUTE FUNCTION "idpay-pagamenti".fn_transaction_outbox();
