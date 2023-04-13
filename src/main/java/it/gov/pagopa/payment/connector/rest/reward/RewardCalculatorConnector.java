package it.gov.pagopa.payment.connector.rest.reward;

import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.dto.RewardPreview;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface RewardCalculatorConnector {

  AuthPaymentResponseDTO authorizePayment(
      @PathVariable("initiativeId") String initiativeId, @RequestBody AuthPaymentRequestDTO body);

  RewardPreview previewTransaction(
      @PathVariable("initiativeId") String initiativeId, @RequestBody AuthPaymentRequestDTO body);
}
