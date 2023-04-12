package it.gov.pagopa.payment.connector.rest.reward;

import feign.FeignException;
import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.connector.rest.reward.mapper.AuthPaymentRequestMapper;
import it.gov.pagopa.payment.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.exception.ClientExceptionWithBody;
import org.springframework.http.HttpStatus;

public class RewardCalculatorConnectorImpl implements RewardCalculatorConnector {

  private final RewardCalculatorRestClient restClient;
  private final AuthPaymentRequestMapper requestMapper;

  public RewardCalculatorConnectorImpl(RewardCalculatorRestClient restClient,
      AuthPaymentRequestMapper requestMapper) {
    this.restClient = restClient;
    this.requestMapper = requestMapper;
  }

  @Override
  @PerformanceLog("QR_CODE_AUTHORIZE_TRANSACTION_REWARD_CALCULATOR")
  public AuthPaymentResponseDTO authorizePayment(String initiativeId, AuthPaymentRequestDTO body) {
    return restClient.authorizePayment(initiativeId,body);
  }

  @Override
  @PerformanceLog("QR_CODE_PREVIEW_TRANSACTION_REWARD_CALCULATOR")
  public AuthPaymentResponseDTO previewTransaction(
      String initiativeId, AuthPaymentRequestDTO body) {
    AuthPaymentResponseDTO response = new AuthPaymentResponseDTO();
    try{
      response = restClient.previewTransaction(initiativeId, body);
    } catch (FeignException e) {
      switch (e.status()) {
        case 403, 409 -> {
          return response;
        }
        case 429 ->
            throw new ClientExceptionWithBody(HttpStatus.TOO_MANY_REQUESTS, "REWARD CALCULATOR",
                "Too many request on the ms reward");
        default -> throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }
    return response;
  }
}