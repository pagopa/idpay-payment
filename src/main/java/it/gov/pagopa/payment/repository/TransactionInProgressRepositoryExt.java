package it.gov.pagopa.payment.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.model.TransactionInProgress;
import java.util.List;

public interface TransactionInProgressRepositoryExt {
  UpdateResult createIfExists(TransactionInProgress trx, String trxCode);
  TransactionInProgress findByTrxCodeThrottled(String trxCode);
  void updateTrxAuthorized(String id, Reward reward, List<String> rejectionReasons);
  TransactionInProgress findByTrxCodeAndTrxChargeDateNotExpired(String trxCode);
  void updateTrxRejected(String id, String userId, List<String> rejectionReasons);
  void updateTrxIdentified(String id, String userId);
}
