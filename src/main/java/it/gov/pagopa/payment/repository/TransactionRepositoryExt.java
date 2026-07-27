package it.gov.pagopa.payment.repository;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TransactionRepositoryExt {

  @Query(value = """
  WITH selected AS (
      SELECT id
      FROM transaction
      WHERE
          "trxDate" < :expirationDate
          AND status IN (:statusList)
          AND "extendedAuthorization" IS FALSE
          AND (
              "elaborationDateTime" IS NULL
              OR "elaborationDateTime" < CURRENT_TIMESTAMP - (:throttlingMillis || ' milliseconds')::interval
          )
          AND (CAST(:initiativeId AS varchar) IS NULL OR "initiativeId" = :initiativeId)
      ORDER BY "trxDate" ASC
      LIMIT 1
      FOR UPDATE SKIP LOCKED
  )
  UPDATE transaction t
  SET "elaborationDateTime" = (NOW() AT TIME ZONE 'Europe/Rome')
  FROM selected
  WHERE t.id = selected.id
  RETURNING t.*
  """,
          nativeQuery = true)
  Transaction findAuthorizationExpiredTransaction(
          @Param("initiativeId") String initiativeId,
          @Param("expirationDate") LocalDateTime expirationDate,
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

  @Query(value = """
        UPDATE transaction t SET
            "trxChargeDate" = (NOW() AT TIME ZONE 'Europe/Rome'),
            "updateDate" = (NOW() AT TIME ZONE 'Europe/Rome')
        WHERE t."trxCode" = :trxCode
            AND t."trxDate" > :minTrxDate
            AND (t."trxChargeDate" IS NULL OR t."trxChargeDate" < (NOW() AT TIME ZONE 'Europe/Rome') - INTERVAL '10 second')
        RETURNING t.*
        """, nativeQuery = true)
  Optional<Transaction> findAndModifyThrottled(
          @Param("trxCode") String trxCode,
          @Param("minTrxDate") LocalDateTime minTrxDate
  );

  @Query(value = """
        UPDATE transaction 
        SET "elaborationDateTime" = (NOW() AT TIME ZONE 'Europe/Rome')
        WHERE id = (
          SELECT t.id 
          FROM transaction t
          WHERE t."trxDate" < :maxTrxDate
            AND t.status IN (:statusList)
            AND (t."extendedAuthorization" IS NOT TRUE)
            AND (CAST(:initiativeId AS varchar) IS NULL OR t."initiativeId" = :initiativeId)
            AND (
                 t."elaborationDateTime" IS NULL
                 OR t."elaborationDateTime" < ((NOW() AT TIME ZONE 'Europe/Rome') - (:throttlingSeconds || ' second')::interval)
            )
          ORDER BY t."trxDate" ASC
          LIMIT 1
          FOR UPDATE SKIP LOCKED
        ) 
        RETURNING *
        """, nativeQuery = true)
  Optional<Transaction> findAndModifyExpiredTransaction(
          @Param("maxTrxDate") LocalDateTime maxTrxDate,
          @Param("statusList") List<String> statusList,
          @Param("initiativeId") String initiativeId,
          @Param("throttlingSeconds") int throttlingSeconds
  );

  @Modifying
  @Transactional
  @Query(value = """
        UPDATE transaction 
        SET status = 'EXPIRED', 
            "updateDate" = (NOW() AT TIME ZONE 'Europe/Rome') 
        WHERE "initiativeId" = :initiativeId 
          AND status = 'CREATED' 
          AND "trxEndDate" IS NOT NULL 
          AND "extendedAuthorization" = TRUE 
          AND ("trxEndDate" < :now OR "initiativeName" IS NOT NULL) 
          AND ("userId" IS NULL OR "userId" NOT IN (
              SELECT DISTINCT t2."userId" 
              FROM transaction t2 
              WHERE t2."initiativeId" = :initiativeId 
                AND t2.status = 'AUTHORIZED' 
                AND t2."userId" IS NOT NULL
          ))
        """, nativeQuery = true)
  int updateStatusForExpiredVoucherTransactions(
          @Param("initiativeId") String initiativeId,
          @Param("now") LocalDateTime now
  );

  @Modifying
  @Transactional
  @Query("""
        UPDATE Transaction t
        SET t.status = :newStatus,
            t.rejectionReasons = :rejectionReasons,
            t.updateDate = CURRENT_TIMESTAMP
        WHERE t.id = :trxId 
          AND t.status = :expectedStatus
    """)
  int updateTrxPostTimeout(
          @Param("trxId") String trxId,
          @Param("expectedStatus") SyncTrxStatus expectedStatus,
          @Param("newStatus") SyncTrxStatus newStatus,
          @Param("rejectionReasons") List<String> rejectionReasons
  );

  @Query(value = """
      DELETE FROM transaction
      WHERE id IN (
          SELECT id 
          FROM transaction
          WHERE "initiativeId" = :initiativeId
          LIMIT :pageSize
          FOR UPDATE SKIP LOCKED
      )
      RETURNING *
      """, nativeQuery = true)
  List<Transaction> deletePaged(
          @Param("initiativeId") String initiativeId,
          @Param("pageSize") int pageSize
  );
}
