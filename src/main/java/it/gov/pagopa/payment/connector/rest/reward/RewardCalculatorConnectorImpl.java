package it.gov.pagopa.payment.connector.rest.reward;

import feign.FeignException;
import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.dto.RewardPreview;
import it.gov.pagopa.payment.dto.mapper.AuthPaymentResponseDTO2RewardPreviewMapper;
import it.gov.pagopa.payment.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.exception.ClientExceptionWithBody;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RewardCalculatorConnectorImpl implements RewardCalculatorConnector {

  private final RewardCalculatorRestClient restClient;
  private final AuthPaymentResponseDTO2RewardPreviewMapper authPaymentResponseDTO2RewardPreviewMapper;

  public RewardCalculatorConnectorImpl(RewardCalculatorRestClient restClient,
      AuthPaymentResponseDTO2RewardPreviewMapper authPaymentResponseDTO2RewardPreviewMapper) {
    this.restClient = restClient;
    this.authPaymentResponseDTO2RewardPreviewMapper = authPaymentResponseDTO2RewardPreviewMapper;
  }

  @Override
  @PerformanceLog("QR_CODE_AUTHORIZE_TRANSACTION_REWARD_CALCULATOR")
  public AuthPaymentResponseDTO authorizePayment(String initiativeId, AuthPaymentRequestDTO body) {
    AuthPaymentResponseDTO responseDTO = new AuthPaymentResponseDTO();
    try {
      responseDTO = restClient.authorizePayment(initiativeId, body);
    } catch (FeignException e) {
      switch (e.status()) {
        case 409 -> {
          return responseDTO;
        }
        case 429 ->
            throw new ClientExceptionWithBody(HttpStatus.TOO_MANY_REQUESTS, "REWARD CALCULATOR",
                "Too many request in the microservice reward-calculator");
        default -> throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR,
            "An error occurred in the microservice reward-calculator");
      }
    }
    return responseDTO;
  }

  @Override
  @PerformanceLog("QR_CODE_PREVIEW_TRANSACTION_REWARD_CALCULATOR")
  public RewardPreview previewTransaction(
      String initiativeId, AuthPaymentRequestDTO body) {
    AuthPaymentResponseDTO response = new AuthPaymentResponseDTO();
    try{
      response = restClient.previewTransaction(initiativeId, body);
    } catch (FeignException e) {
      switch (e.status()) {
        case 403, 409 -> {
          return authPaymentResponseDTO2RewardPreviewMapper.apply(response);
        }
        case 429 ->
            throw new ClientExceptionWithBody(HttpStatus.TOO_MANY_REQUESTS, "REWARD CALCULATOR",
                "Too many request on the ms reward");
        default -> throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR,
            "An error occurred in the microservice reward-calculator");
      }
    }
    return authPaymentResponseDTO2RewardPreviewMapper.apply(response);
  }
}
