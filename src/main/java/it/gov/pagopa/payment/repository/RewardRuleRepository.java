package it.gov.pagopa.payment.repository;

public interface RewardRuleRepository {
  boolean checkIfExists(String initiativeId);
}
