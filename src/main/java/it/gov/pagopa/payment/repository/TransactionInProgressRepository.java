package it.gov.pagopa.payment.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.model.TransactionInProgress;
import java.util.List;

public interface TransactionInProgressRepository {
  UpdateResult createIfExists(TransactionInProgress trx, String trxCode);
  TransactionInProgress findAndModify(String trxCode);
  void updateTrxAuthorized(String id, Reward reward, List<String> rejectionReasons);
  TransactionInProgress findByIdAndUserId(String id, String userId);
}
