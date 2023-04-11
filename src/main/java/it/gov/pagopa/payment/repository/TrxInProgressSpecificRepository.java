package it.gov.pagopa.payment.repository;

import it.gov.pagopa.payment.model.TransactionInProgress;

public interface TrxInProgressSpecificRepository {

  TransactionInProgress findAndModify(String userId,String trxCode);

}
