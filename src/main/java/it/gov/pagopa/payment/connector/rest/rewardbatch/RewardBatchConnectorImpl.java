package it.gov.pagopa.payment.connector.rest.rewardbatch;

import feign.FeignException;
import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityDTO;
import it.gov.pagopa.payment.exception.custom.RewardBatchInvocationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RewardBatchConnectorImpl implements RewardBatchConnector {

    private final RewardBatchRestClient restClient;

    public RewardBatchConnectorImpl(RewardBatchRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<RewardBatchEligibilityDTO> findEligibility(String merchantId, String transactionId, String authorization) {
        try {
            ResponseEntity<RewardBatchEligibilityDTO> response =
                    restClient.findEligibility(transactionId, merchantId, authorization);
            return Optional.ofNullable(response.getBody());
        } catch (FeignException e) {
            throw new RewardBatchInvocationException("An error occurred in the reward batch eligibility service", true, e);
        }
    }
}
