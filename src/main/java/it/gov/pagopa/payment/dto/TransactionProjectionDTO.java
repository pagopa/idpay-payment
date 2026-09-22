package it.gov.pagopa.payment.dto;

import it.gov.pagopa.payment.model.InvoiceData;

public record TransactionProjectionDTO(
        String transactionId,
        String status,
        InvoiceData invoiceData
) {
}

