package it.gov.pagopa.payment.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

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
  Criteria getCriteria(String merchantId, String initiativeId, String userId, String status);
  List<TransactionInProgress> findByFilter(Criteria criteria, Pageable pageable);
  long getCount(Criteria criteria);
}
