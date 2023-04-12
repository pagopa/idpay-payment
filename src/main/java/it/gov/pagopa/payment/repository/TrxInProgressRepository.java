package it.gov.pagopa.payment.repository;

import it.gov.pagopa.payment.model.TransactionInProgress;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrxInProgressRepository extends MongoRepository<TransactionInProgress, String> {

  Optional<TransactionInProgressRepository> findByIdAndUserId(String id, String userId);

}
