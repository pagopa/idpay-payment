package it.gov.pagopa.payment.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.payment.model.TransactionInProgress;
import java.util.List;

public interface TransactionInProgressRepositoryExt {
  UpdateResult createIfExists(TransactionInProgress trx, String trxCode);
  TransactionInProgress findByIdThrottled(String trxId);
  TransactionInProgress findByTrxCodeAndTrxChargeDateNotExpired(String trxCode);
  TransactionInProgress findByTrxCodeAndTrxChargeDateNotExpiredThrottled(String trxCode);
  void updateTrxRejected(String id, String userId, List<String> rejectionReasons);
  void updateTrxIdentified(String id, String userId);
  void updateTrxAuthorized(String id, Long reward, List<String> rejectionReasons);
  void updateTrxRejected(String id, List<String> rejectionReasons);
}
