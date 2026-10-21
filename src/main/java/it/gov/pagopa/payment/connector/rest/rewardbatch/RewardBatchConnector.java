package it.gov.pagopa.payment.connector.rest.rewardbatch;

import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityDecision;
import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityOperation;

public interface RewardBatchConnector {
    RewardBatchEligibilityDecision getEligibilityDecision(
            String transactionId,
            RewardBatchEligibilityOperation operation,
            String authorization);
}
