package it.gov.pagopa.payment.repository;

import it.gov.pagopa.payment.entity.TransactionOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TransactionOutboxRepository extends JpaRepository<TransactionOutbox, String>{

    List<TransactionOutbox> findTop100ByPublishedFalseOrderByCreatedAtAsc();

    @Modifying
    @Query("""
            update TransactionOutbox t
            set t.published = true
            where t.id = :id
            """)
    void markAsPublished(Long id);
}