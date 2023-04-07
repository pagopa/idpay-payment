package it.gov.pagopa.payment.repository;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import it.gov.pagopa.payment.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

class RewardRuleRepositoryImplTest extends BaseIntegrationTest {

  @Autowired
  protected RewardRuleRepositoryImpl rewardRuleRepository;

  @Autowired
  protected MongoTemplate mongoTemplate;

  @BeforeEach
  void setUp(){
    DBObject initiativeConfig1 = BasicDBObjectBuilder.start().add("initiativeId", "INITIATIVEID1").get();
    DBObject initiative1 = BasicDBObjectBuilder.start().add("id", "TEST_INITIATIVE").add("initiativeConfig", initiativeConfig1).get();
    mongoTemplate.save(initiative1, "reward_rule");
  }

  @AfterEach
  void tearDown(){
    mongoTemplate.remove(Query.query(Criteria.where("id").is("TEST_INITIATIVE")), "reward_rule");
  }

  @Test
  void checkIfExistsTrue(){
    boolean result = rewardRuleRepository.checkIfExists("INITIATIVEID1");
    Assertions.assertTrue(result);
  }

  @Test
  void checkIfExistsFalse(){
    boolean result = rewardRuleRepository.checkIfExists("INITIATIVEID2");
    Assertions.assertFalse(result);
  }
}