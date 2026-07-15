package it.gov.pagopa.payment.repository;

import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String>, TransactionRepositoryExt, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByTrxCode(String trxCode);
    Optional<Transaction> findByInitiativeIdAndTrxCodeAndUserId(String initiativeId, String trxCode, String userId);
    Optional<Transaction> findByIdAndMerchantIdAndStatusIn(String id, String merchantId, Collection<SyncTrxStatus> statuses);
    List<Transaction> findByUserIdAndInitiativeIdAndChannel(String userId, String initiativeId, String channel);
    List<Transaction> findByUserIdAndInitiativeIdAndStatusAndExtendedAuthorizationNot(
            String userId,
            String initiativeId,
            SyncTrxStatus status,
            Boolean extendedAuthorization
    );

}