package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;

public interface TransactionService {
    TransactionInProgress getTransaction(String id, String userId);
    SyncTrxStatus getStatusTransaction(String transactionId, String merchantId);
}
