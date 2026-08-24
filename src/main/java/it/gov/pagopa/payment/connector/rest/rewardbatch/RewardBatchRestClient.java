package it.gov.pagopa.payment.connector.rest.rewardbatch;

import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "reward-batch",
        url = "${rest-client.reward-batch.baseUrl}")
public interface RewardBatchRestClient {

    @GetMapping(
            value = "/idpay/transactions/{transactionId}/reward-batch/eligibility",
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<RewardBatchEligibilityDTO> findEligibility(
            @PathVariable("transactionId") String transactionId,
            @RequestParam("merchantId") String merchantId,
            @RequestHeader("Authorization") String authorization);
}
