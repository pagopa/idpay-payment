package it.gov.pagopa.payment.connector;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.qrcode.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.dto.qrcode.AuthPaymentResponseDTO;

public class RewardCalculatorConnectorImpl implements RewardCalculatorConnector{

  private final RewardCalculatorRestClient restClient;

  public RewardCalculatorConnectorImpl(RewardCalculatorRestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  @PerformanceLog("AUTHORIZE_TRANSACTION_QR_CODE_REWARD_CALCULATOR")
  public AuthPaymentResponseDTO authorizePayment(String initiativeId, AuthPaymentRequestDTO body) {
    return restClient.authorizePayment(initiativeId,body);
  }
}
