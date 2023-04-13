package it.gov.pagopa.payment.connector.rest.reward;

import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.RewardPreview;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface RewardCalculatorConnector {

  AuthPaymentDTO authorizePayment(TransactionInProgress transaction, AuthPaymentRequestDTO body);

  RewardPreview previewTransaction(
      @PathVariable("initiativeId") String initiativeId, @RequestBody AuthPaymentRequestDTO body);
}
