package it.gov.pagopa.payment.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public interface TransactionInProgressRepositoryExt {
  UpdateResult createIfExists(TransactionInProgress trx, String trxCode);
  TransactionInProgress findByIdThrottled(String trxId);
  TransactionInProgress findByTrxCodeAndAuthorizationNotExpired(String trxCode, long authorizationExpirationMinutes);
  TransactionInProgress findByTrxIdAndAuthorizationNotExpired(String trxId, long authorizationExpirationMinutes);
  TransactionInProgress findByTrxCodeAndAuthorizationNotExpiredThrottled(String trxCode, long authorizationExpirationMinutes);
  void updateTrxRejected(String id, String userId, List<String> rejectionReasons, Map<String, List<String>> initiativeRejectionReason, String channel);
  void updateTrxRelateUserIdentified(String id, String userId, String channel);
  @SuppressWarnings("squid:S00107")
  void updateTrxWithStatus(String id, String userId, Long reward, List<String> rejectionReasons, Map<String, List<String>> initiativeRejectionReasons, Map<String, Reward> rewards, String channel, SyncTrxStatus status, long counterVersion, OffsetDateTime trxChargeDate, Long amountCents);
  UpdateResult updateTrxAuthorized(TransactionInProgress trx, AuthPaymentDTO authPaymentDTO, Map<String, List<String>> initiativeRejectionReasons);
  void updateTrxRejected(TransactionInProgress trx, List<String> rejectionReasons, Map<String, List<String>> initiativeRejectionReason);
  Criteria getCriteria(String merchantId, String initiativeId, String userId, String status);
  List<TransactionInProgress> findByFilter(Criteria criteria, Pageable pageable);
  long getCount(Criteria criteria);
  TransactionInProgress findCancelExpiredTransaction(String initiativeId, long cancelExpirationMinutes);
  TransactionInProgress findAuthorizationExpiredTransaction(String initiativeId, long authorizationExpirationMinutes);
  List<TransactionInProgress> deletePaged(String initiativeId, int pageSize);
  UpdateResult updateTrxPostTimeout(String trxId);
}
