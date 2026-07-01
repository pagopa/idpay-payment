package it.gov.pagopa.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionOutboxRepository extends JpaRepository<TransactionOutbox, String>{

}