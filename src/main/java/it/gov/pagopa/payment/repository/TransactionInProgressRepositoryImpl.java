package it.gov.pagopa.payment.repository;

import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TransactionInProgressRepositoryImpl implements
    TransactionInProgressRepository {

  private final MongoTemplate mongoTemplate;

  public TransactionInProgressRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public TransactionInProgress createIfNotExists(TransactionInProgress trx) {
    return mongoTemplate.insert(trx);
  }
}
