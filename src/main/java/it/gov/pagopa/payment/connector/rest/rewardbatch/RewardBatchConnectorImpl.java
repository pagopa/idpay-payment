package it.gov.pagopa.payment.connector.rest.rewardbatch;

import feign.FeignException;
import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityDecision;
import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityOperation;
import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityRequestDTO;
import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityResponseDTO;
import it.gov.pagopa.payment.exception.custom.RewardBatchInvocationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class RewardBatchConnectorImpl implements RewardBatchConnector {

    private final RewardBatchRestClient restClient;

    public RewardBatchConnectorImpl(RewardBatchRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public RewardBatchEligibilityDecision getEligibilityDecision(
            String transactionId,
            RewardBatchEligibilityOperation operation,
            String authorization) {
        try {
            ResponseEntity<RewardBatchEligibilityResponseDTO> response =
                    restClient.getEligibilityDecision(
                            transactionId,
                            new RewardBatchEligibilityRequestDTO(operation),
                            authorization);
            RewardBatchEligibilityResponseDTO body = response == null ? null : response.getBody();
            if (body == null || body.getDecision() == null) {
                throw new RewardBatchInvocationException(
                        "The reward batch eligibility service returned an invalid decision", false, null);
            }
            return body.getDecision();
        } catch (FeignException e) {
            throw new RewardBatchInvocationException("An error occurred in the reward batch eligibility service", true, e);
        }
    }
}
