package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.model.TransactionInProgress;

public interface TransactionInProgressService {
    void generateTrxCodeAndSave(TransactionInProgress trx, String flowName);
}
