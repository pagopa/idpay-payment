package it.gov.pagopa.payment.connector.rest.reward;

import it.gov.pagopa.payment.dto.qrcode.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.dto.qrcode.AuthPaymentResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(
    name = "${rest-client.reward.payment}",
    url = "${rest-client.reward.baseUrl}")
public interface RewardCalculatorRestClient {

  @PutMapping(
      value = "idpay/reward/{initiativeId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  AuthPaymentResponseDTO authorizePayment(@PathVariable("initiativeId") String initiativeId,
      @RequestBody AuthPaymentRequestDTO body);

}
