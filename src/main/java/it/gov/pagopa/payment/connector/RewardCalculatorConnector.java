package it.gov.pagopa.payment.connector;

import it.gov.pagopa.payment.dto.qrcode.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.dto.qrcode.AuthPaymentResponseDTO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface RewardCalculatorConnector {

  AuthPaymentResponseDTO authorizePayment(@PathVariable("initiativeId") String initiativeId,
      @RequestBody AuthPaymentRequestDTO body);

}
