package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityOperation;
import it.gov.pagopa.payment.entity.Transaction;

public interface RewardBatchEligibilityPreflightService {
    void verifyEligibility(
            Transaction transaction,
            RewardBatchEligibilityOperation operation,
            String authorization);
}
