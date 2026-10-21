package it.gov.pagopa.payment.repository;

import it.gov.pagopa.payment.entity.Transaction;

public interface InvoiceTransactionRepository {

    Transaction updateInvoiceAndCreateEvent(InvoiceTransactionCommand command);
}
