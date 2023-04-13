package it.gov.pagopa.payment.connector.rest.reward;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.connector.rest.reward.mapper.AuthPaymentMapper;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RewardCalculatorConnectorImpl implements RewardCalculatorConnector {

  private final RewardCalculatorRestClient restClient;
  private final ObjectMapper objectMapper;
  private final AuthPaymentMapper requestMapper;

  public RewardCalculatorConnectorImpl(RewardCalculatorRestClient restClient,
      ObjectMapper objectMapper, AuthPaymentMapper requestMapper) {
    this.restClient = restClient;
    this.objectMapper = objectMapper;
    this.requestMapper = requestMapper;
  }

  @Override
  @PerformanceLog("QR_CODE_AUTHORIZE_TRANSACTION_REWARD_CALCULATOR")
  public AuthPaymentDTO authorizePayment(TransactionInProgress transaction, AuthPaymentRequestDTO body) {
    AuthPaymentResponseDTO responseDTO;
    try {
      responseDTO = restClient.authorizePayment(transaction.getInitiativeId(), body);
    } catch (FeignException e) {
      switch (e.status()) {
        case 409 -> {
          try {
            responseDTO = objectMapper.readValue(e.contentUTF8(), AuthPaymentResponseDTO.class);
          } catch (JsonProcessingException ex) {
            throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong");
          }
        }
        case 429 ->
            throw new ClientExceptionWithBody(HttpStatus.TOO_MANY_REQUESTS, "REWARD CALCULATOR",
                "Too many request in the microservice reward-calculator");
        default -> throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR,
            "An error occurred in the microservice reward-calculator");
      }
    }
    return requestMapper.rewardResponseMap(responseDTO, transaction);
  }

  @Override
  @PerformanceLog("QR_CODE_PREVIEW_TRANSACTION_REWARD_CALCULATOR")
  public AuthPaymentDTO previewTransaction(
      TransactionInProgress transaction, AuthPaymentRequestDTO body) {
    AuthPaymentResponseDTO responseDTO;
    try{
      responseDTO = restClient.previewTransaction(transaction.getInitiativeId(), body);
    } catch (FeignException e) {
      switch (e.status()) {
        case 403, 409 -> {
          try {
            responseDTO = objectMapper.readValue(e.contentUTF8(), AuthPaymentResponseDTO.class);
          } catch (JsonProcessingException ex) {
            throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong");
          }
        }
        case 429 ->
            throw new ClientExceptionWithBody(HttpStatus.TOO_MANY_REQUESTS, "REWARD CALCULATOR",
                "Too many request on the ms reward");
        default -> throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR,
            "An error occurred in the microservice reward-calculator");
      }
    }
    return requestMapper.rewardResponseMap(responseDTO, transaction);
  }
}
