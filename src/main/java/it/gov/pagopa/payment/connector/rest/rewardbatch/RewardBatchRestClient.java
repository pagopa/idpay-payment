package it.gov.pagopa.payment.connector.rest.rewardbatch;

import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityRequestDTO;
import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "reward-batch",
        url = "${rest-client.reward-batch.baseUrl}")
public interface RewardBatchRestClient {

    @PostMapping(
            value = "/idpay/transactions/{transactionId}/invoice-lifecycle/eligibility",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<RewardBatchEligibilityResponseDTO> getEligibilityDecision(
            @PathVariable("transactionId") String transactionId,
            @RequestBody RewardBatchEligibilityRequestDTO request,
            @RequestHeader("Authorization") String authorization);
}
