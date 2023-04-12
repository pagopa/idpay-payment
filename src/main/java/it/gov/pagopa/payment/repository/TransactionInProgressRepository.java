package it.gov.pagopa.payment.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.payment.model.TransactionInProgress;

public interface TransactionInProgressRepository {
  UpdateResult createIfExists(TransactionInProgress trx, String trxCode);

  TransactionInProgress findById(String trxId);
}
