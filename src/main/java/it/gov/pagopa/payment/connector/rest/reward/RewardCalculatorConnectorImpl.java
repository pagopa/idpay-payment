package it.gov.pagopa.payment.connector.rest.reward;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.PaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.mapper.RewardCalculatorMapper;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.*;
import it.gov.pagopa.payment.model.TransactionInProgress;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.apache.commons.lang3.function.TriFunction;

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
        return performRequest(trx, (initiativeId, body) -> {

            ResponseEntity<AuthPaymentResponseDTO> response = restClient.previewTransaction(initiativeId, body);
            AuthPaymentResponseDTO authPaymentResponseDTO = response.getBody();

            try {
                String etagHeaderValue = response.getHeaders().getFirst(HttpHeaders.ETAG);
                if (etagHeaderValue != null) {
                    //noinspection DataFlowIssue
                    authPaymentResponseDTO.setCounterVersion(Long.parseLong(etagHeaderValue));
                }
            } catch (NumberFormatException e) {
                throw new RewardCalculatorInvocationException("Error parsing ETAG from headers", true, e);
            }
            return authPaymentResponseDTO;
        });
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
            result = performCancel(trx, restClient::cancelTransaction);
        } catch (TransactionNotFoundOrExpiredException ex) {
            result = null;
        }
        return result;
    }

    private AuthPaymentDTO performRequest(TransactionInProgress trx, BiFunction<String, PaymentRequestDTO, AuthPaymentResponseDTO> requestExecutor){
        return performRequest(trx, ()-> requestExecutor.apply(trx.getInitiativeId(), requestMapper.preAuthRequestMap(trx)));
    }
    private AuthPaymentDTO performCancel(TransactionInProgress trx, BiFunction<String, AuthPaymentRequestDTO, AuthPaymentResponseDTO> requestExecutor){
        return performRequest(trx, ()-> requestExecutor.apply(trx.getInitiativeId(), requestMapper.authRequestMap(trx)));
    }
    private AuthPaymentDTO performRequest(TransactionInProgress trx, TriFunction<Long,String, AuthPaymentRequestDTO, AuthPaymentResponseDTO> requestExecutor){
        return performRequest(trx, ()-> requestExecutor.apply(trx.getCounterVersion(),trx.getInitiativeId(), requestMapper.authRequestMap(trx)));
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
                        throw new RewardCalculatorInvocationException("Something went wrong", true, ex);
                    }
                }
                case 429 -> throw new TooManyRequestsException(
                        "Too many request on the ms reward",true,e);
                case 404 -> throw new TransactionNotFoundOrExpiredException(
                        "Resource not found on reward-calculator", true, e);
                case 412 ->{
                        responseDTO = new AuthPaymentResponseDTO();
                        responseDTO.setStatus(SyncTrxStatus.REJECTED);
                        responseDTO.setRejectionReasons(List.of(PaymentConstants.ExceptionCode.PAYMENT_CANNOT_GUARANTEE_REWARD));
                        responseDTO.setInitiativeId(trx.getInitiativeId());
                }
                case 423 -> throw new TransactionVersionPendingException(
                        "The transaction version is actually locked", true,e);
                default -> throw new RewardCalculatorInvocationException(
                        "An error occurred in the microservice reward-calculator", true, e);
            }
        }
        return requestMapper.rewardResponseMap(responseDTO, trx);
    }
}
