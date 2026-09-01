package it.gov.pagopa.payment.repository;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.enums.TransactionEventType;
import it.gov.pagopa.payment.model.InvoiceData;

import java.time.LocalDateTime;
import java.util.Objects;

public record InvoiceTransactionCommand(
        String transactionId,
        String initiativeId,
        String merchantId,
        SyncTrxStatus expectedStatus,
        long expectedRevision,
        InvoiceData invoiceData,
        LocalDateTime updateDate,
        String franchiseName,
        String pointOfSaleType,
        String businessName,
        String merchantFiscalCode,
        TransactionEventType eventType
) {
    public InvoiceTransactionCommand {
        Objects.requireNonNull(transactionId);
        Objects.requireNonNull(initiativeId);
        Objects.requireNonNull(merchantId);
        Objects.requireNonNull(expectedStatus);
        Objects.requireNonNull(invoiceData);
        Objects.requireNonNull(updateDate);
        Objects.requireNonNull(eventType);

        boolean validClassification = switch (expectedStatus) {
            case CAPTURED -> eventType == TransactionEventType.TRANSACTION_INVOICED;
            case INVOICED, REWARDED ->
                    eventType == TransactionEventType.TRANSACTION_INVOICE_REPLACED;
            default -> false;
        };
        if (!validClassification) {
            throw new IllegalArgumentException(
                    "Event type [%s] is not valid for invoice status [%s]"
                            .formatted(eventType, expectedStatus));
        }
        if (expectedRevision < 0) {
            throw new IllegalArgumentException("Expected transaction revision must not be negative");
        }
    }
}
