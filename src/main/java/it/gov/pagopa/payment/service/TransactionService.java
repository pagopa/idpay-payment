package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.model.TransactionInProgress;

public interface TransactionService {
    TransactionInProgress getTransaction(String id, String userId);
}
