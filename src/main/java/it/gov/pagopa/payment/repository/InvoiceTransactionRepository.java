package it.gov.pagopa.payment.repository;

import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.enums.TransactionEventType;

import java.time.LocalDateTime;

public interface InvoiceTransactionRepository {

    Transaction updateInvoiceAndCreateEvent(InvoiceTransactionCommand command);

    /**
     * [BND-2132] Applies a status-only transition (no invoice/merchant fields) and emits the
     * matching typed outbox event in the same transaction, so the state change is auditable and
     * propagated. Returns {@code null} when the transaction id is not found (no event emitted).
     */
    Transaction updateStatusAndCreateEvent(
            String transactionId,
            SyncTrxStatus newStatus,
            TransactionEventType eventType,
            LocalDateTime updateDate);
}
