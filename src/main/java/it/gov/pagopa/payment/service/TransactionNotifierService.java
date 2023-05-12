package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.model.TransactionInProgress;

public interface TransactionNotifierService {
    boolean notify(TransactionInProgress trx, String key);
}
