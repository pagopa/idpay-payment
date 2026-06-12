package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.entity.Transaction;

public interface TransactionService {
    void generateTrxCodeAndSave(Transaction trx, String flowName);

}
