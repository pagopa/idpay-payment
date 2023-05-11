package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.model.TransactionInProgress;

public interface TransactionNotifierService {
    boolean notifyByMerch(TransactionInProgress trx);
    boolean notifyByUser(TransactionInProgress trx);
}
