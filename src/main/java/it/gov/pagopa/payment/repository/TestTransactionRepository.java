package it.gov.pagopa.payment.repository;

import it.gov.pagopa.payment.entity.Transaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@ConditionalOnProperty(name = "app.test-support.enabled", havingValue = "true")
public interface TestTransactionRepository extends Repository<Transaction, String> {

    @Modifying
    @Transactional
    @Query("""
        UPDATE Transaction t
        SET t.trxEndDate = :newEndDate
        WHERE t.id = :id
          AND t.status = 'CREATED'
          AND t.initiativeId = :initiativeId
          AND t.userId = :userId
        """)
    int updateTrxEndDate(
            @Param("id") String id,
            @Param("initiativeId") String initiativeId,
            @Param("userId") String userId,
            @Param("newEndDate") OffsetDateTime newEndDate
    );
}
