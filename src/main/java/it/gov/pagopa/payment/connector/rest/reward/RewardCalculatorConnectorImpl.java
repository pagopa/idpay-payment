package it.gov.pagopa.payment.connector.rest.reward;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.connector.rest.reward.mapper.RewardCalculatorMapper;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RewardCalculatorConnectorImpl implements RewardCalculatorConnector {

  private final RewardCalculatorRestClient restClient;
  private final ObjectMapper objectMapper;
  private final RewardCalculatorMapper requestMapper;

  public RewardCalculatorConnectorImpl(RewardCalculatorRestClient restClient,
      ObjectMapper objectMapper, RewardCalculatorMapper requestMapper) {
    this.restClient = restClient;
    this.objectMapper = objectMapper;
    this.requestMapper = requestMapper;
  }

  @Override
  @PerformanceLog("QR_CODE_AUTHORIZE_TRANSACTION_REWARD_CALCULATOR")
  public AuthPaymentDTO authorizePayment(TransactionInProgress trx) {
    AuthPaymentRequestDTO request = requestMapper.rewardMap(trx);

    AuthPaymentResponseDTO responseDTO;
    try {
      responseDTO = restClient.authorizePayment(trx.getInitiativeId(), request);
    } catch (FeignException e) {
      switch (e.status()) {
        case 409 -> {
          try {
            responseDTO = objectMapper.readValue(e.contentUTF8(), AuthPaymentResponseDTO.class);
          } catch (JsonProcessingException ex) {
            throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong", ex);
          }
        }
        case 429 ->
            throw new ClientExceptionWithBody(HttpStatus.TOO_MANY_REQUESTS, "REWARD CALCULATOR",
                "Too many request in the microservice reward-calculator");
        default -> throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR,
            "An error occurred in the microservice reward-calculator", e);
      }
    }
    return requestMapper.rewardResponseMap(responseDTO, trx);
  }

  @Override
  @PerformanceLog("QR_CODE_PREVIEW_TRANSACTION_REWARD_CALCULATOR")
  public AuthPaymentDTO previewTransaction(TransactionInProgress trx) {
    AuthPaymentRequestDTO request = requestMapper.rewardMap(trx);

    AuthPaymentResponseDTO responseDTO;
    try{
      responseDTO = restClient.previewTransaction(trx.getInitiativeId(), request);
    } catch (FeignException e) {
      switch (e.status()) {
        case 403, 409 -> {
          try {
            responseDTO = objectMapper.readValue(e.contentUTF8(), AuthPaymentResponseDTO.class);
          } catch (JsonProcessingException ex) {
            throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong", ex);
          }
        }
        case 429 ->
            throw new ClientExceptionWithBody(HttpStatus.TOO_MANY_REQUESTS, "REWARD CALCULATOR",
                "Too many request on the ms reward");
        default -> throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR,
            "An error occurred in the microservice reward-calculator", e);
      }
    }
    return requestMapper.rewardResponseMap(responseDTO, trx);
  }
}
