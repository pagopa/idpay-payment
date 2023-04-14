package it.gov.pagopa.payment.connector.rest.reward;

import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(
    name = "${rest-client.reward.payment}",
    url = "${rest-client.reward.baseUrl}")
public interface RewardCalculatorRestClient {

  @PostMapping(
      value = "reward/{initiativeId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  AuthPaymentResponseDTO authorizePayment(@PathVariable("initiativeId") String initiativeId,
      @RequestBody AuthPaymentRequestDTO body);

  @PostMapping(
      value = "reward/preview/{initiativeId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  AuthPaymentResponseDTO previewTransaction(@PathVariable("initiativeId") String initiativeId,
      @RequestBody AuthPaymentRequestDTO body);

}
