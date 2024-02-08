package it.gov.pagopa.payment.connector.rest.reward;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.connector.rest.reward.mapper.RewardCalculatorMapper;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.exception.custom.RewardCalculatorInvocationException;
import it.gov.pagopa.payment.exception.custom.TooManyRequestsException;
import it.gov.pagopa.payment.model.TransactionInProgress;

import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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
        AuthPaymentDTO result;
        try {
            result = performRequest(trx, () -> restClient.cancelTransaction(trx.getId()));
        } catch (TransactionNotFoundOrExpiredException ex) {
            result = null;
        }
        return result;
    }

    private AuthPaymentDTO performRequest(TransactionInProgress trx, BiFunction<String, AuthPaymentRequestDTO, Object> requestExecutor){
        return performRequest(trx, ()-> requestExecutor.apply(trx.getInitiativeId(), requestMapper.rewardMap(trx)));
    }

    private AuthPaymentDTO performRequest(TransactionInProgress trx, Supplier<Object> requestExecutor) {
        Object response;
        AuthPaymentResponseDTO responseDTO;
        try {
            response = requestExecutor.get();
            // Preview transaction case
            if( response instanceof ResponseEntity<?> entity)
                responseDTO = extractResponseFromBody(entity);
            // Authorize payment case
            else if(response instanceof AuthPaymentResponseDTO authPaymentResponseDTO)
                responseDTO = authPaymentResponseDTO;
            else
                throw new RewardCalculatorInvocationException("Invalid response type");
        } catch (FeignException e) {
            switch (e.status()) {
                case 403, 409 -> {
                    try {
                        responseDTO = objectMapper.readValue(e.contentUTF8(), AuthPaymentResponseDTO.class);
                    } catch (JsonProcessingException ex) {
                        throw new RewardCalculatorInvocationException("Something went wrong", true, ex);
                    }
                }
                case 429 -> throw new TooManyRequestsException(
                        "Too many request on the ms reward",true,e);
                case 404 -> throw new TransactionNotFoundOrExpiredException(
                        "Resource not found on reward-calculator", true, e);
                default -> throw new RewardCalculatorInvocationException(
                        "An error occurred in the microservice reward-calculator", true, e);
            }
        }
        return requestMapper.rewardResponseMap(responseDTO, trx);
    }

    private AuthPaymentResponseDTO extractResponseFromBody(ResponseEntity<?> entity) {
        AuthPaymentResponseDTO responseDTO;
        Object body = entity.getBody();
        HttpHeaders headers = entity.getHeaders();
        long etag;

        if (body instanceof AuthPaymentResponseDTO authPaymentResponseDTO)
            responseDTO = authPaymentResponseDTO;
        else
            throw new RewardCalculatorInvocationException("Invalid response body type");

        try {
            String etagHeaderValue = headers.getFirst(HttpHeaders.ETAG);
            if (etagHeaderValue != null) {
                etag = Long.parseLong(etagHeaderValue);
            } else {
                throw new RewardCalculatorInvocationException("ETAG header not found");
            }
        } catch (NumberFormatException | NoSuchElementException e) {
            throw new RewardCalculatorInvocationException("Error parsing ETAG from headers", true, e);
        }

        responseDTO.setCounterVersion(etag);
        return  responseDTO;
    }
}
