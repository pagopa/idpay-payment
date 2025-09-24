package it.gov.pagopa.payment.repository;

import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionInProgressRepository  extends MongoRepository<TransactionInProgress, String>, TransactionInProgressRepositoryExt {
    Optional<TransactionInProgress> findByIdAndMerchantIdAndAcquirerId(String id, String merchantId, String acquirerId);
    Optional<TransactionInProgress> findByTrxCode(String trxCode);
    List<TransactionInProgress> deleteByInitiativeId(String initiativeId);
    List<TransactionInProgress> findByUserIdAndInitiativeIdAndChannel(String userId, String initiativeId, String channel);
}