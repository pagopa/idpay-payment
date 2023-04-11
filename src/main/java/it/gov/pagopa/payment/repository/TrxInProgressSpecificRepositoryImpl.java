package it.gov.pagopa.payment.repository;

import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.model.TransactionInProgress.Fields;
import java.time.LocalDateTime;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class TrxInProgressSpecificRepositoryImpl implements TrxInProgressSpecificRepository {

  private final MongoTemplate mongoTemplate;


  public TrxInProgressSpecificRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public TransactionInProgress findAndModify(String userId, String trxCode) {
    return mongoTemplate.findAndModify(
        Query.query(Criteria.where(Fields.trxCode).is(trxCode).and(Fields.userId).is(userId)
            .andOperator(Criteria.where(Fields.authDate).exists(false)
                    .orOperator(
                        Criteria.where(Fields.authDate).lte(LocalDateTime.now().minusSeconds(15)),
                        Criteria.where(Fields.authDate).is(null)
                    ),
                Criteria.where(Fields.trxDate).gte(LocalDateTime.now().minusMinutes(15))
            )), new Update().set(Fields.authDate, LocalDateTime.now()),
        FindAndModifyOptions.options().returnNew(true), TransactionInProgress.class);
  }
}
