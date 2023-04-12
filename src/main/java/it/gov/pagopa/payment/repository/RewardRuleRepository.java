package it.gov.pagopa.payment.repository;

import it.gov.pagopa.payment.model.RewardRule;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RewardRuleRepository extends MongoRepository<RewardRule, String> {}
