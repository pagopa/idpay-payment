package it.gov.pagopa.payment.connector;

import it.gov.pagopa.payment.dto.qrcode.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.dto.qrcode.AuthPaymentResponseDTO;

public class RewardCalculatorConnectorImpl implements RewardCalculatorConnector{

  private final RewardCalculatorRestClient restClient;

  public RewardCalculatorConnectorImpl(RewardCalculatorRestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public AuthPaymentResponseDTO authorizePayment(String initiativeId, AuthPaymentRequestDTO body) {
    return restClient.authorizePayment(initiativeId,body);
  }
}
