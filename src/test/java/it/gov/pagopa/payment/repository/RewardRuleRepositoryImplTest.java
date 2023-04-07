package it.gov.pagopa.payment.repository;

import it.gov.pagopa.payment.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RewardRuleRepositoryImplTest extends BaseIntegrationTest {

  @Autowired
  protected RewardRuleRepositoryImpl rewardRuleRepository;

  @AfterEach
  void tearDown() {

  }

  @Test
  void checkIfExistsTrue(){
    boolean result = rewardRuleRepository.checkIfExists("INITIATIVEID1");
    Assertions.assertFalse(result);
  }
}