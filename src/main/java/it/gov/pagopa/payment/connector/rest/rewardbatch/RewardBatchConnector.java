package it.gov.pagopa.payment.connector.rest.rewardbatch;

import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityDTO;

import java.util.Optional;

public interface RewardBatchConnector {
    Optional<RewardBatchEligibilityDTO> findEligibility(String merchantId, String transactionId, String authorization);
}
