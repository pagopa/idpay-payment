package it.gov.pagopa.payment.connector.rest.reward;

import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    name = "reward-calculator",
    url = "${rest-client.reward.baseUrl}")
public interface RewardCalculatorRestClient {

  @PostMapping(
          value = "reward/initiative/preview/{initiativeId}",
          produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  AuthPaymentResponseDTO previewTransaction(@PathVariable("initiativeId") String initiativeId,
                                            @RequestBody AuthPaymentRequestDTO body);

  @PostMapping(
      value = "reward/initiative/{initiativeId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  AuthPaymentResponseDTO authorizePayment(@PathVariable("initiativeId") String initiativeId,
      @RequestBody AuthPaymentRequestDTO body);

  @DeleteMapping(
      value = "reward/{transactionId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  AuthPaymentResponseDTO cancelTransaction(@PathVariable("transactionId") String transactionId);

}
