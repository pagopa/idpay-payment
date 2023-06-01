package it.gov.pagopa.payment.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.model.TransactionInProgress;
import java.util.List;
import java.util.Map;

public interface TransactionInProgressRepositoryExt {
  UpdateResult createIfExists(TransactionInProgress trx, String trxCode);
  TransactionInProgress findByIdThrottled(String trxId);
  TransactionInProgress findByTrxCodeAndTrxChargeDateNotExpired(String trxCode);
  TransactionInProgress findByTrxCodeAndTrxChargeDateNotExpiredThrottled(String trxCode);
  void updateTrxRejected(String id, String userId, List<String> rejectionReasons);
  void updateTrxIdentified(String id, String userId, Long reward, List<String> rejectionReasons, Map<String, Reward> rewards);
  void updateTrxAuthorized(TransactionInProgress trx, Long reward, List<String> rejectionReasons);
  void updateTrxRejected(String id, List<String> rejectionReasons);
}
