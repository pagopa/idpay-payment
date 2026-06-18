package it.gov.pagopa.payment.repository;

import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    Optional<Transaction> findByIdAndTrxChargeDateGreaterThanEqual(String id, OffsetDateTime trxChargeDate);

    Optional<Transaction> findByTrxCode(String trxCode);

    Optional<Transaction> findByTrxCodeAndTrxEndDateGreaterThanEqual(String trxCode, OffsetDateTime trxEndDate);

    Optional<Transaction> findByInitiativeIdAndTrxCodeAndUserId(String initiativeId, String trxCode, String userId);
        
    List<Transaction> findByUserIdAndInitiativeIdAndChannel(
            String userId,
            String initiativeId,
            String channel
    );

    List<Transaction> findByUserIdAndInitiativeIdAndStatusAndExtendedAuthorizationNot(
            String userId,
            String initiativeId,
            SyncTrxStatus status,
            Boolean extendedAuthorization
    );

    List<Transaction> findByStatusAndUpdateDateBefore(
            SyncTrxStatus status,
            LocalDateTime updateDate,
            Pageable pageable
    );

    @Query("""
    select t
    from Transaction t
    where t.trxEndDate < :now
      and t.status in :statuses
      and (t.extendedAuthorization is null or t.extendedAuthorization = false)
      and (:initiativeId is null or t.initiativeId = :initiativeId)
    order by t.trxDate asc
""")
    List<Transaction> findLapsedTransactions(
            @Param("initiativeId") String initiativeId,
            @Param("now") OffsetDateTime now,
            @Param("statuses") List<SyncTrxStatus> statuses,
            Pageable pageable
    );

    List<Transaction> findByStatusOrderByTrxDateAsc(
            SyncTrxStatus status,
            Pageable pageable
    );

    @Modifying
    @Query("""
    delete from Transaction t
    where t.id in :ids
""")
    int bulkDeleteByIds(@Param("ids") List<String> ids);

    @Modifying
    @Query("""
    update Transaction t
    set
        t.status = :status,
        t.rewardCents = :rewardCents,
        t.rejectionReasons = :rejectionReasons,
        t.initiativeRejectionReasons = :initiativeRejectionReasons,
        t.rewards = :rewards,
        t.trxChargeDate = :trxChargeDate,
        t.counterVersion = :counterVersion,
        t.familyId = :familyId,
        t.updateDate = :updateDate
    where
        t.id = :id
        and t.status = :expectedStatus
""")
    int updateAuthorized(
            @Param("id") String id,
            @Param("expectedStatus") SyncTrxStatus expectedStatus,
            @Param("status") SyncTrxStatus status,
            @Param("rewardCents") Long rewardCents,
            @Param("rejectionReasons") List<String> rejectionReasons,
            @Param("initiativeRejectionReasons") Map<String, List<String>> initiativeRejectionReasons,
            @Param("rewards") Object rewards,
            @Param("trxChargeDate") OffsetDateTime trxChargeDate,
            @Param("counterVersion") Long counterVersion,
            @Param("familyId") String familyId,
            @Param("updateDate") LocalDateTime updateDate
    );

    @Modifying
    @Query("""
    update Transaction t
    set
        t.status = :status,
        t.rewardCents = :rewardCents,
        t.rewards = :rewards,
        t.rejectionReasons = :rejectionReasons,
        t.initiativeRejectionReasons = :initiativeRejectionReasons,
        t.trxChargeDate = :trxChargeDate,
        t.updateDate = :updateDate,
        t.amountCurrency = :amountCurrency,
        t.amountCents = :amountCents,
        t.effectiveAmountCents = :effectiveAmountCents,
        t.idTrxAcquirer = :idTrxAcquirer,
        t.merchantId = :merchantId,
        t.businessName = :businessName,
        t.vat = :vat,
        t.merchantFiscalCode = :merchantFiscalCode,
        t.acquirerId = :acquirerId
    where t.id = :id
""")
    int updateRejected(
            @Param("id") String id,
            @Param("status") SyncTrxStatus status,
            @Param("rewardCents") Long rewardCents,
            @Param("rewards") Object rewards,
            @Param("rejectionReasons") List<String> rejectionReasons,
            @Param("initiativeRejectionReasons") Map<String, List<String>> initiativeRejectionReasons,
            @Param("trxChargeDate") OffsetDateTime trxChargeDate,
            @Param("updateDate") LocalDateTime updateDate,
            @Param("amountCurrency") String amountCurrency,
            @Param("amountCents") Long amountCents,
            @Param("effectiveAmountCents") Long effectiveAmountCents,
            @Param("idTrxAcquirer") String idTrxAcquirer,
            @Param("merchantId") String merchantId,
            @Param("businessName") String businessName,
            @Param("vat") String vat,
            @Param("merchantFiscalCode") String merchantFiscalCode,
            @Param("acquirerId") String acquirerId
    );
}