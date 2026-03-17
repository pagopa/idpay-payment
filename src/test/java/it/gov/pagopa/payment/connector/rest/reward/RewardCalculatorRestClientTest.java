package it.gov.pagopa.payment.connector.rest.reward;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.connector.rest.reward.mapper.RewardCalculatorMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.*;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.AuthPaymentResponseDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import static it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode.PAYMENT_CANNOT_GUARANTEE_REWARD;
import static it.gov.pagopa.payment.constants.PaymentConstants.REWARD_CALCULATOR_TRX_ALREADY_AUTHORIZED;
import static it.gov.pagopa.payment.constants.PaymentConstants.REWARD_CALCULATOR_TRX_ALREADY_CANCELLED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@Slf4j
class RewardCalculatorRestClientTest{

    private RewardCalculatorRestClient rewardCalculatorRestClient;

    private RewardCalculatorConnector rewardCalculatorConnector;

    private ObjectMapper objectMapper;

    private RewardCalculatorMapper rewardCalculatorMapper;

    @BeforeEach
    void setUp() {
        rewardCalculatorRestClient = mock(RewardCalculatorRestClient.class);
        objectMapper = mock(ObjectMapper.class);
        rewardCalculatorMapper = mock(RewardCalculatorMapper.class);
        rewardCalculatorConnector = new RewardCalculatorConnectorImpl(rewardCalculatorRestClient, objectMapper, rewardCalculatorMapper);
    }

    @Test
    void authorizePayment_ok(){
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        AuthPaymentResponseDTO authPaymentResponseDTO = AuthPaymentResponseDTOFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        AuthPaymentRequestDTO authPaymentRequestDTO = new AuthPaymentRequestDTO();
        when(rewardCalculatorRestClient.authorizePayment(
                1L, "INITIATIVE_ID", authPaymentRequestDTO
        )).thenReturn(authPaymentResponseDTO);

        assertDoesNotThrow(() -> rewardCalculatorConnector.authorizePayment(transaction));
    }

    @Test
    void cancelTransaction_ok(){
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        AuthPaymentResponseDTO authPaymentResponseDTO = AuthPaymentResponseDTOFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        AuthPaymentRequestDTO authPaymentRequestDTO = new AuthPaymentRequestDTO();
        when(rewardCalculatorRestClient.cancelTransaction(
                "INITIATIVE_ID", authPaymentRequestDTO
        )).thenReturn(authPaymentResponseDTO);

        assertDoesNotThrow(() -> rewardCalculatorConnector.cancelTransaction(transaction));
    }

    @Test
    void previewTransaction_ok(){
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        AuthPaymentResponseDTO authPaymentResponseDTO = AuthPaymentResponseDTOFaker.mockInstance(1,SyncTrxStatus.AUTHORIZED);
        when(rewardCalculatorRestClient.previewTransaction(anyString(),any())).thenReturn(ResponseEntity.ok(authPaymentResponseDTO));
        assertDoesNotThrow(() -> rewardCalculatorConnector.previewTransaction(transaction));
    }

    @Test
    void previewTransaction_etagHeaderValueIsNotNull(){
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        AuthPaymentResponseDTO authPaymentResponseDTO = AuthPaymentResponseDTOFaker.mockInstance(1,SyncTrxStatus.AUTHORIZED);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ETAG, "10");

        ResponseEntity<AuthPaymentResponseDTO> response =
                ResponseEntity.ok()
                        .headers(headers)
                        .body(authPaymentResponseDTO);

        when(rewardCalculatorRestClient.previewTransaction(anyString(),any())).thenReturn(response);
        assertDoesNotThrow(() -> rewardCalculatorConnector.previewTransaction(transaction));
    }

    @Test
    void previewTransaction_throwException(){
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        AuthPaymentResponseDTO authPaymentResponseDTO = AuthPaymentResponseDTOFaker.mockInstance(1,SyncTrxStatus.AUTHORIZED);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ETAG, "LONG");

        ResponseEntity<AuthPaymentResponseDTO> response =
                ResponseEntity.ok()
                        .headers(headers)
                        .body(authPaymentResponseDTO);

        when(rewardCalculatorRestClient.previewTransaction(anyString(),any())).thenReturn(response);

        RewardCalculatorInvocationException ex = assertThrows(RewardCalculatorInvocationException.class,
                () -> rewardCalculatorConnector.previewTransaction(transaction));
        assertNotNull(ex.getMessage());
    }

    @Test
    void previewTransaction_403(){
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);

        when(rewardCalculatorRestClient.previewTransaction(anyString(),any())).thenThrow(buildFeignException(403,"body"));

        assertDoesNotThrow(() -> rewardCalculatorConnector.previewTransaction(transaction));
    }

    @Test
    void previewTransaction_429(){
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);

        when(rewardCalculatorRestClient.previewTransaction(anyString(),any())).thenThrow(buildFeignException(429,"body"));

        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> rewardCalculatorConnector.previewTransaction(transaction));
        assertNotNull(ex.getMessage());
    }

    @Test
    void previewTransaction_404(){
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);

        when(rewardCalculatorRestClient.previewTransaction(anyString(),any())).thenThrow(buildFeignException(404,"body"));

        TransactionNotFoundOrExpiredException ex = assertThrows(TransactionNotFoundOrExpiredException.class,
                () -> rewardCalculatorConnector.previewTransaction(transaction));
        assertNotNull(ex.getMessage());
    }

    @Test
    void previewTransaction_412(){
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);

        when(rewardCalculatorRestClient.previewTransaction(anyString(),any())).thenThrow(buildFeignException(412,"body"));

        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> rewardCalculatorConnector.previewTransaction(transaction));
        assertNotNull(ex.getMessage());
    }

    @Test
    void previewTransaction_423(){
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);

        when(rewardCalculatorRestClient.previewTransaction(anyString(),any())).thenThrow(buildFeignException(423,"body"));

        TransactionVersionPendingException ex = assertThrows(TransactionVersionPendingException.class,
                () -> rewardCalculatorConnector.previewTransaction(transaction));
        assertNotNull(ex.getMessage());
    }

    @Test
    void previewTransaction_500(){
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);

        when(rewardCalculatorRestClient.previewTransaction(anyString(),any())).thenThrow(buildFeignException(500,"body"));

        RewardCalculatorInvocationException ex = assertThrows(RewardCalculatorInvocationException.class,
                () -> rewardCalculatorConnector.previewTransaction(transaction));
        assertNotNull(ex.getMessage());
    }

    @Test
    void previewPayment_403_readValueEx() throws Exception {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        String body = "{\"code\":\"" + REWARD_CALCULATOR_TRX_ALREADY_AUTHORIZED + "\"}";
        FeignException ex = buildFeignException(403, body);

        JsonProcessingException jsonProcessingException = mock(JsonProcessingException.class);

        when(objectMapper.readValue(anyString(), ArgumentMatchers.eq(AuthPaymentResponseDTO.class))).thenThrow(jsonProcessingException);

        when(rewardCalculatorRestClient.previewTransaction(anyString(), any())).thenThrow(ex);

        assertThrows(RewardCalculatorInvocationException.class,
                () -> rewardCalculatorConnector.previewTransaction(trx));
    }

    @Test
    void previewPayment_412_readValueEx() throws Exception {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        String body = "{\"code\":\"" + REWARD_CALCULATOR_TRX_ALREADY_AUTHORIZED + "\"}";
        FeignException ex = buildFeignException(412, body);

        JsonProcessingException jsonProcessingException = mock(JsonProcessingException.class);

        when(objectMapper.readValue(anyString(), ArgumentMatchers.eq(ErrorDTO.class))).thenThrow(jsonProcessingException);

        when(rewardCalculatorRestClient.previewTransaction(anyString(), any())).thenThrow(ex);

        assertThrows(RewardCalculatorInvocationException.class,
                () -> rewardCalculatorConnector.previewTransaction(trx));
    }

    @Test
    void previewPayment_412_alreadyAuthorized() throws Exception {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        String body = "{\"code\":\"" + REWARD_CALCULATOR_TRX_ALREADY_AUTHORIZED + "\"}";
        FeignException ex = buildFeignException(412, body);

        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage(REWARD_CALCULATOR_TRX_ALREADY_AUTHORIZED);
        errorDTO.setCode(REWARD_CALCULATOR_TRX_ALREADY_AUTHORIZED);

        when(objectMapper.readValue(anyString(), ArgumentMatchers.eq(ErrorDTO.class))).thenReturn(errorDTO);

        when(rewardCalculatorRestClient.previewTransaction(anyString(), any())).thenThrow(ex);

        assertThrows(TransactionAlreadyAuthorizedException.class,
                () -> rewardCalculatorConnector.previewTransaction(trx));
    }

    @Test
    void previewPayment_412_alreadyCancelled() throws Exception {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        String body = "{\"code\":\"" + REWARD_CALCULATOR_TRX_ALREADY_CANCELLED + "\"}";
        FeignException ex = buildFeignException(412, body);

        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage(REWARD_CALCULATOR_TRX_ALREADY_CANCELLED);
        errorDTO.setCode(REWARD_CALCULATOR_TRX_ALREADY_CANCELLED);

        when(objectMapper.readValue(anyString(), ArgumentMatchers.eq(ErrorDTO.class))).thenReturn(errorDTO);

        when(rewardCalculatorRestClient.previewTransaction(anyString(), any())).thenThrow(ex);

        assertThrows(TransactionAlreadyCancelledException.class,
                () -> rewardCalculatorConnector.previewTransaction(trx));
    }

    @Test
    void previewPayment_412_alreadyRejected() throws Exception {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        String body = "{\"code\":\"" + PAYMENT_CANNOT_GUARANTEE_REWARD + "\"}";
        FeignException ex = buildFeignException(412, body);

        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage(PAYMENT_CANNOT_GUARANTEE_REWARD);
        errorDTO.setCode(PAYMENT_CANNOT_GUARANTEE_REWARD);

        when(objectMapper.readValue(anyString(), ArgumentMatchers.eq(ErrorDTO.class))).thenReturn(errorDTO);

        when(rewardCalculatorRestClient.previewTransaction(anyString(), any())).thenThrow(ex);

        assertDoesNotThrow(() -> rewardCalculatorConnector.previewTransaction(trx));
    }

    private FeignException buildFeignException(int status, String body) {
        return FeignException.errorStatus(
                "methodKey",
                feign.Response.builder()
                        .status(status)
                        .reason("reason")
                        .request(feign.Request.create(
                                feign.Request.HttpMethod.GET,
                                "/test",
                                java.util.Collections.emptyMap(),
                                null,
                                java.nio.charset.StandardCharsets.UTF_8,
                                null))
                        .body(body, java.nio.charset.StandardCharsets.UTF_8)
                        .build()
        );
    }

}