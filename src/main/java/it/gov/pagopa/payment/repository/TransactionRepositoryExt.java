package it.gov.pagopa.payment.repository;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TransactionRepositoryExt {

  @Query(value = """
    WITH selected AS (
        SELECT id
        FROM Transaction
        WHERE
            trx_date < :expirationDate
            AND status = ANY(:statusList)
            AND extended_authorization IS FALSE
            AND (
                elaboration_date_time IS NULL
                OR elaboration_date_time < CURRENT_TIMESTAMP - (:throttlingMillis || ' milliseconds')::interval
            )
            AND (:initiativeId IS NULL OR initiative_id = :initiativeId)
        ORDER BY trx_date
        LIMIT 1
        FOR UPDATE SKIP LOCKED
    )
    UPDATE Transaction t
    SET elaboration_date_time = NOW()
    FROM selected
    WHERE t.id = selected.id
    RETURNING t
    """,
          nativeQuery = true)
  Transaction findAuthorizationExpiredTransaction(
          @Param("initiativeId") String initiativeId,
          @Param("expirationDate") OffsetDateTime expirationDate,
          @Param("statusList") List<String> statusList,
          @Param("throttlingMillis") long throttlingMillis
  );


  @Modifying
  @Transactional
  @Query("UPDATE Transaction t SET " +
          "t.status = :status, " +
          "t.rewardCents = 0, " +
          "t.rewards = null, " +
          "t.rejectionReasons = :rejectionReasons, " +
          "t.initiativeRejectionReasons = :initiativeRejectionReasons, " +
          "t.trxChargeDate = :#{#trx.trxChargeDate}, " +
          "t.updateDate = :updateDate, " +
          // Logica BARCODE usando l'oggetto trx passato
          "t.amountCurrency = CASE WHEN t.channel = 'BARCODE' THEN :currency ELSE t.amountCurrency END, " +
          "t.amountCents = CASE WHEN t.channel = 'BARCODE' THEN :#{#trx.amountCents} ELSE t.amountCents END, " +
          "t.effectiveAmountCents = CASE WHEN t.channel = 'BARCODE' THEN :#{#trx.effectiveAmountCents} ELSE t.effectiveAmountCents END, " +
          "t.idTrxAcquirer = CASE WHEN t.channel = 'BARCODE' THEN :#{#trx.idTrxAcquirer} ELSE t.idTrxAcquirer END, " +
          "t.merchantId = CASE WHEN t.channel = 'BARCODE' THEN :#{#trx.merchantId} ELSE t.merchantId END, " +
          "t.businessName = CASE WHEN t.channel = 'BARCODE' THEN :#{#trx.businessName} ELSE t.businessName END, " +
          "t.vat = CASE WHEN t.channel = 'BARCODE' THEN :#{#trx.vat} ELSE t.vat END, " +
          "t.merchantFiscalCode = CASE WHEN t.channel = 'BARCODE' THEN :#{#trx.merchantFiscalCode} ELSE t.merchantFiscalCode END, " +
          "t.acquirerId = CASE WHEN t.channel = 'BARCODE' THEN :#{#trx.acquirerId} ELSE t.acquirerId END " +
          "WHERE t.id = :#{#trx.id}")
  void updateTrxRejected(
          @Param("trx") Transaction trx,
          @Param("status") SyncTrxStatus status,
          @Param("rejectionReasons") List<String> rejectionReasons,
          @Param("initiativeRejectionReasons") Map<String, List<String>> initiativeRejectionReasons,
          @Param("updateDate") LocalDateTime updateDate,
          @Param("currency") String currency
  );

  @Modifying
  @Transactional
  @Query("UPDATE Transaction t SET " +
          "t.status = :status, " +
          "t.userId = :userId, " +
          "t.rewardCents = 0, " +
          "t.rewards = null, " +
          "t.rejectionReasons = :rejectionReasons, " +
          "t.initiativeRejectionReasons = :initiativeRejectionReasons, " +
          "t.channel = :channel, " +
          "t.updateDate = :updateDate " +
          "WHERE t.id = :id")
  void updateTrxRejected(
          @Param("id") String id,
          @Param("userId") String userId,
          @Param("status") SyncTrxStatus status,
          @Param("rejectionReasons") List<String> rejectionReasons,
          @Param("initiativeRejectionReasons") Map<String, List<String>> initiativeRejectionReasons,
          @Param("channel") String channel,
          @Param("updateDate") LocalDateTime updateDate
  );

  @Modifying
  @Transactional
  @Query("UPDATE Transaction t SET " +
          "t.status = :status, " +
          "t.userId = :#{#trx.userId}, " +
          "t.rewardCents = :#{#preview.rewardCents}, " +
          "t.rejectionReasons = :#{#preview.rejectionReasons}, " +
          "t.initiativeRejectionReasons = :initiativeRejectionReasons, " +
          "t.rewards = :#{#preview.rewards}, " +
          "t.channel = :channel, " +
          "t.counterVersion = :#{#preview.counterVersion}, " +
          "t.trxChargeDate = :#{#trx.trxChargeDate}, " +
          "t.amountCents = :#{#trx.amountCents}, " +
          "t.merchantId = :#{#trx.merchantId}, " +
          "t.updateDate = :updateDate " +
          "WHERE t.id = :#{#trx.id}")
  void updateTrxWithStatusForPreview(
          @Param("trx") Transaction trx,
          @Param("preview") AuthPaymentDTO preview,
          @Param("initiativeRejectionReasons") Map<String, List<String>> initiativeRejectionReasons,
          @Param("channel") String channel,
          @Param("status") SyncTrxStatus status,
          @Param("updateDate") LocalDateTime updateDate
  );

  @Modifying
  @Transactional
  @Query("UPDATE Transaction t SET " +
          "t.status = :status, " +
          "t.rewardCents = :#{#dto.rewardCents}, " +
          "t.rejectionReasons = :#{#dto.rejectionReasons}, " +
          "t.initiativeRejectionReasons = :initiativeRejectionReasons, " +
          "t.rewards = :#{#dto.rewards}, " +
          "t.trxChargeDate = :#{#trx.trxChargeDate}, " +
          "t.counterVersion = :#{#dto.counters != null ? #dto.counters.version : null}, " +
          "t.familyId = :#{#trx.familyId}, " +
          "t.updateDate = :updateDate, " +
          // Logica BARCODE
          "t.amountCurrency = CASE WHEN t.channel = 'BARCODE' THEN :currency ELSE t.amountCurrency END, " +
          "t.amountCents = CASE WHEN t.channel = 'BARCODE' THEN :#{#trx.amountCents} ELSE t.amountCents END, " +
          "t.effectiveAmountCents = CASE WHEN t.channel = 'BARCODE' THEN :#{#trx.effectiveAmountCents} ELSE t.effectiveAmountCents END, " +
          "t.idTrxAcquirer = CASE WHEN t.channel = 'BARCODE' THEN :#{#trx.idTrxAcquirer} ELSE t.idTrxAcquirer END, " +
          "t.merchantId = CASE WHEN t.channel = 'BARCODE' THEN :#{#trx.merchantId} ELSE t.merchantId END, " +
          "t.businessName = CASE WHEN t.channel = 'BARCODE' THEN :#{#trx.businessName} ELSE t.businessName END, " +
          "t.vat = CASE WHEN t.channel = 'BARCODE' THEN :#{#trx.vat} ELSE t.vat END, " +
          "t.merchantFiscalCode = CASE WHEN t.channel = 'BARCODE' THEN :#{#trx.merchantFiscalCode} ELSE t.merchantFiscalCode END, " +
          "t.acquirerId = CASE WHEN t.channel = 'BARCODE' THEN :#{#trx.acquirerId} ELSE t.acquirerId END " +
          "WHERE t.id = :#{#trx.id} AND t.status = :requiredStatus")
  int updateTrxAuthorized(
          @Param("trx") Transaction trx,
          @Param("dto") AuthPaymentDTO dto,
          @Param("initiativeRejectionReasons") Map<String, List<String>> initiativeRejectionReasons,
          @Param("requiredStatus") SyncTrxStatus requiredStatus,
          @Param("status") SyncTrxStatus status,
          @Param("updateDate") LocalDateTime updateDate,
          @Param("currency") String currency
  );


  @Modifying
  @Transactional
  @Query("UPDATE Transaction t SET " +
          "t.status = :#{#trx.status}, " +
          "t.userId = :#{#trx.userId}, " +
          "t.rewardCents = :#{#trx.rewardCents}, " +
          "t.rejectionReasons = :#{#trx.rejectionReasons}, " +
          "t.initiativeRejectionReasons = :#{#trx.initiativeRejectionReasons}, " +
          "t.rewards = :#{#trx.rewards}, " +
          "t.channel = :#{#trx.channel}, " +
          "t.counterVersion = :#{#trx.counterVersion}, " +
          "t.trxChargeDate = :#{#trx.trxChargeDate}, " +
          "t.amountCents = :#{#trx.amountCents}, " +
          "t.merchantId = :#{#trx.merchantId}, " +
          "t.additionalProperties = :#{#trx.additionalProperties}, " +
          "t.pointOfSaleId = :#{#trx.pointOfSaleId}, " +
          "t.franchiseName = :#{#trx.franchiseName}, " +
          "t.pointOfSaleType = :#{#trx.pointOfSaleType}, " +
          "t.updateDate = :updateDate " +
          "WHERE t.id = :#{#trx.id}")
  void updateTrxWithStatus(@Param("trx") Transaction trx, @Param("updateDate") LocalDateTime updateDate);

  @Modifying
  @Transactional
  @Query("DELETE FROM Transaction t WHERE t.id IN :ids")
  void bulkDeleteByIds(@Param("ids") List<String> ids);

  @Query("SELECT t FROM Transaction t WHERE t.trxCode = :trxCode AND t.trxEndDate >= :now")
  Optional<Transaction> findByTrxCodeAndAuthorizationNotExpired(
          @Param("trxCode") String trxCode,
          @Param("now") OffsetDateTime now
  );

  @Query(value = "UPDATE transaction t SET " +
          "  trx_charge_date = NOW(), " +
          "  update_date = NOW() " +
          "WHERE t.trx_code = :trxCode " +
          "  AND t.trx_date > :minTrxDate " +
          "  AND (t.trx_charge_date IS NULL OR t.trx_charge_date < NOW() - INTERVAL '10 second') " +
          "RETURNING *", nativeQuery = true)
  Optional<Transaction> findAndModifyThrottled(
          @Param("trxCode") String trxCode,
          @Param("minTrxDate") OffsetDateTime minTrxDate
  );

  @Query("SELECT COUNT(t) > 0 FROM Transaction t WHERE t.trxCode = :trxCode AND t.trxDate > :minTrxDate")
  boolean existsByTrxCodeAndDateGreaterThan(
          @Param("trxCode") String trxCode,
          @Param("minTrxDate") OffsetDateTime minTrxDate
  );

  @Query(value =
          "UPDATE transaction SET elaboration_date = NOW() " +
                  "WHERE id = (" +
                  "  SELECT t.id FROM transaction t " +
                  "  WHERE t.trx_date < :maxTrxDate " +
                  "    AND t.status IN (:statusList) " +
                  "    AND (t.extended_authorization IS NOT TRUE) " +
                  "    AND (:initiativeId IS NULL OR t.initiative_id = :initiativeId) " +
                  "    AND ( " +
                  "         t.elaboration_date IS NULL " +
                  "         OR t.elaboration_date < (NOW() - CAST(:throttlingSeconds || ' second' AS INTERVAL)) " +
                  "    ) " +
                  "  ORDER BY t.trx_date ASC " +
                  "  LIMIT 1 " +
                  "  FOR UPDATE SKIP LOCKED" +
                  ") RETURNING *",
          nativeQuery = true)
  Optional<Transaction> findAndModifyExpiredTransaction(
          @Param("maxTrxDate") OffsetDateTime maxTrxDate,
          @Param("statusList") List<String> statusList,
          @Param("initiativeId") String initiativeId,
          @Param("throttlingSeconds") int throttlingSeconds
  );

  @Query("SELECT t FROM Transaction t WHERE t.id = :trxId AND t.trxDate >= :minTrxDate")
  Optional<Transaction> findByTrxIdAndAuthorizationNotExpired(
          @Param("trxId") String trxId,
          @Param("minTrxDate") OffsetDateTime minTrxDate
  );

  @Modifying
  @Transactional
  @Query(value =
          "UPDATE transaction " +
                  "SET status = 'EXPIRED', update_date = NOW() " +
                  "WHERE initiative_id = :initiativeId " +
                  "  AND status = 'CREATED' " +
                  "  AND trx_end_date IS NOT NULL " +
                  "  AND extended_authorization = TRUE " +
                  "  AND (trx_end_date < :now OR initiative_name IS NOT NULL) " + // Nota sotto per initiative_end_date
                  "  AND (user_id IS NULL OR user_id NOT IN (" +
                  "      SELECT DISTINCT t2.user_id " +
                  "      FROM transaction t2 " +
                  "      WHERE t2.initiative_id = :initiativeId " +
                  "        AND t2.status = 'AUTHORIZED' " +
                  "        AND t2.user_id IS NOT NULL" +
                  "  ))",
          nativeQuery = true)
  int updateStatusForExpiredVoucherTransactions(
          @Param("initiativeId") String initiativeId,
          @Param("now") OffsetDateTime now
  );
}
