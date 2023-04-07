package it.gov.pagopa.payment.repository;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class RewardRuleRepositoryImpl implements RewardRuleRepository {

  private final MongoTemplate mongoTemplate;

  public RewardRuleRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public boolean checkIfExists(String initiativeId) {
    return mongoTemplate.exists(
        Query.query(Criteria.where("initiativeConfig.initiativeId").is(initiativeId)),
        "reward_rule");
  }
}
