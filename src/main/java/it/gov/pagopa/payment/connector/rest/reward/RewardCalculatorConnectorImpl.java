package it.gov.pagopa.payment.connector.rest.reward;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.connector.rest.reward.mapper.RewardCalculatorMapper;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.function.BiFunction;
import java.util.function.Supplier;

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
    @PerformanceLog("PREVIEW_TRANSACTION_REWARD_CALCULATOR")
    public AuthPaymentDTO previewTransaction(TransactionInProgress trx) {
        return performRequest(trx, restClient::previewTransaction);
    }

    @Override
    @PerformanceLog("AUTHORIZE_TRANSACTION_REWARD_CALCULATOR")
    public AuthPaymentDTO authorizePayment(TransactionInProgress trx) {
        return performRequest(trx, restClient::authorizePayment);
    }

    @Override
    @PerformanceLog("CANCEL_TRANSACTION_REWARD_CALCULATOR")
    public AuthPaymentDTO cancelTransaction(TransactionInProgress trx) {
        try {
            return performRequest(trx, () -> restClient.cancelTransaction(trx.getId()));
        } catch (ClientExceptionNoBody e){
            if(HttpStatus.NOT_FOUND.equals(e.getHttpStatus())){
                return null;
            } else {
                throw e;
            }
        }
    }

    private AuthPaymentDTO performRequest(TransactionInProgress trx, BiFunction<String, AuthPaymentRequestDTO, AuthPaymentResponseDTO> requestExecutor){
        return performRequest(trx, ()-> requestExecutor.apply(trx.getInitiativeId(), requestMapper.rewardMap(trx)));
    }

    private AuthPaymentDTO performRequest(TransactionInProgress trx, Supplier<AuthPaymentResponseDTO> requestExecutor) {
        AuthPaymentResponseDTO responseDTO;
        try {
            responseDTO = requestExecutor.get();
        } catch (FeignException e) {
            switch (e.status()) {
                case 403, 409 -> {
                    try {
                        responseDTO = objectMapper.readValue(e.contentUTF8(), AuthPaymentResponseDTO.class);
                    } catch (JsonProcessingException ex) {
                        throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong", ex);
                    }
                }
                case 429 -> throw new ClientExceptionWithBody(HttpStatus.TOO_MANY_REQUESTS, PaymentConstants.ExceptionCode.TOO_MANY_REQUESTS,
                        "Too many request on the ms reward");
                case 404 -> throw new ClientExceptionNoBody(HttpStatus.NOT_FOUND,
                        "Resource not found on reward-calculator", e);
                default -> throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR,
                        "An error occurred in the microservice reward-calculator", e);
            }
        }
        return requestMapper.rewardResponseMap(responseDTO, trx);
    }
}
