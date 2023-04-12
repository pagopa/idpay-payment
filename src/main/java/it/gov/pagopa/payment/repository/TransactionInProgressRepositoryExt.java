package it.gov.pagopa.payment.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.model.TransactionInProgress;
import java.util.List;

public interface TransactionInProgressRepositoryExt {
  UpdateResult createIfExists(TransactionInProgress trx, String trxCode);
  TransactionInProgress findByTrxCodeThrottled(String trxCode);
  void updateTrxAuthorized(String id, Reward reward, List<String> rejectionReasons);
  TransactionInProgress findByIdAndUserId(String id, String userId);
  TransactionInProgress findByTrxCodeAndRelateUser(String trxCode, String userId);

  void updateTrxRejected(String id, List<String> rejectionReasons);

  void updateTrxIdentified(String id);
}
