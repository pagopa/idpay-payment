package it.gov.pagopa.payment.repository;

import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TransactionInProgressRepository extends
    MongoRepository<TransactionInProgress, String>, TransactionInProgressRepositoryExt {

}