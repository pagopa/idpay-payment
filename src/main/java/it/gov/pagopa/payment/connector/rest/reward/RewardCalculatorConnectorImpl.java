package it.gov.pagopa.payment.connector.rest.reward;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.connector.rest.reward.mapper.AuthPaymentRequestMapper;

public class RewardCalculatorConnectorImpl implements RewardCalculatorConnector {

  private final RewardCalculatorRestClient restClient;
  private final AuthPaymentRequestMapper requestMapper;

  public RewardCalculatorConnectorImpl(RewardCalculatorRestClient restClient,
      AuthPaymentRequestMapper requestMapper) {
    this.restClient = restClient;
    this.requestMapper = requestMapper;
  }

  @Override
  @PerformanceLog("AUTHORIZE_TRANSACTION_QR_CODE_REWARD_CALCULATOR")
  public AuthPaymentResponseDTO authorizePayment(String initiativeId, AuthPaymentRequestDTO body) {
    return restClient.authorizePayment(initiativeId,body);
  }
}
