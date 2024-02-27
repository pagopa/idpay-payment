package it.gov.pagopa.payment.connector.rest.reward;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import it.gov.pagopa.payment.configuration.FeignConfig;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.connector.rest.reward.mapper.RewardCalculatorMapper;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.*;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.AuthPaymentResponseDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureWireMock(stubs = "classpath:/stub", port = 0)
@ContextConfiguration(
        classes = {
                RewardCalculatorConnectorImpl.class,
                FeignAutoConfiguration.class,
                FeignConfig.class,
                HttpMessageConvertersAutoConfiguration.class,
                RewardCalculatorMapper.class,
                WireMockServer.class
        })
@TestPropertySource(
        properties = {"spring.application.name=idpay-reward-calculator",
        "rest-client.reward.baseUrl=http://localhost:${wiremock.server.port}"})
@Slf4j
class RewardCalculatorRestClientTest {

    @Autowired
    private RewardCalculatorRestClient rewardCalculatorRestClient;

    @Autowired
    private RewardCalculatorConnector rewardCalculatorConnector;

    @Autowired
    private WireMockServer wireMockServer;

    @MockBean
    ObjectMapper objectMapper;

    @Test
    void testAuthThenReturnTransactionVersionMismatchException(){
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1,SyncTrxStatus.AUTHORIZATION_REQUESTED);
        trx.setVat("MISMATCH");
        trx.setChannel("QRCODE");
        trx.setInitiativeId("INITIATIVEID_VERSION_MISMATCH");
        trx.setUserId("USERID1");

        AuthPaymentDTO result = rewardCalculatorConnector.authorizePayment(trx);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getRejectionReasons().contains(PaymentConstants.ExceptionCode.PAYMENT_CANNOT_GUARANTEE_REWARD));
        Assertions.assertEquals(SyncTrxStatus.REJECTED,result.getStatus());
    }

    @Test
    void testAuthThenReturnTransactionVersionPendingException(){
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1,SyncTrxStatus.AUTHORIZATION_REQUESTED);
        trx.setVat("PENDING");
        trx.setChannel("QRCODE");
        trx.setInitiativeId("INITIATIVEID_VERSION_PENDING");
        trx.setUserId("USERID2");

        TransactionVersionPendingException exception = Assert.assertThrows(TransactionVersionPendingException.class, () -> rewardCalculatorConnector.authorizePayment(trx));
        Assertions.assertEquals(PaymentConstants.ExceptionCode.PAYMENT_TRANSACTION_VERSION_PENDING,exception.getCode());
        Assertions.assertEquals("The transaction version is actually locked",exception.getMessage());

    }
    @Test
    void testPreviewTransactionResponseWithEtag() {
        TransactionInProgress trx =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        AuthPaymentDTO preview =
                rewardCalculatorConnector.previewTransaction(trx);
        assertNotNull(preview);
        assertNotEquals(0, preview.getCounterVersion());
    }

    @Test
    void testPreviewTransactionResponseWithoutEtag() {
        TransactionInProgress trx =
                TransactionInProgressFaker.mockInstance(3, SyncTrxStatus.CREATED);
        AuthPaymentDTO preview =
                rewardCalculatorConnector.previewTransaction(trx);
        assertNotNull(preview);
        assertEquals(0, preview.getCounterVersion());
    }
    @Test
    void testPreviewTransactionResponseWithBadEtag() {
        TransactionInProgress trx =
                TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.CREATED);

        RewardCalculatorInvocationException exception = assertThrows(RewardCalculatorInvocationException.class,
                () -> rewardCalculatorConnector.previewTransaction(trx));
        assertEquals(PaymentConstants.ExceptionCode.GENERIC_ERROR, exception.getCode());

    }

    @Test
    void testPreviewTransaction_NOT_FOUND() {
        TransactionInProgress trx =
                TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.CREATED);

        TransactionNotFoundOrExpiredException exception = assertThrows(TransactionNotFoundOrExpiredException.class,
                () -> rewardCalculatorConnector.previewTransaction(trx));
        assertEquals(PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, exception.getCode());
    }

    @Test
    void testPreviewTransaction_FORBIDDEN_KO() throws JsonProcessingException {
        TransactionInProgress trx =
                TransactionInProgressFaker.mockInstance(7, SyncTrxStatus.CREATED);

        doThrow(JsonProcessingException.class)
                .when(objectMapper).readValue(anyString(), ArgumentMatchers.eq(AuthPaymentResponseDTO.class));

        RewardCalculatorInvocationException exception = assertThrows(RewardCalculatorInvocationException.class,
                () -> rewardCalculatorConnector.previewTransaction(trx));
        assertEquals(PaymentConstants.ExceptionCode.GENERIC_ERROR, exception.getCode());
    }

    @Test
    void testPreviewTransaction_FORBIDDEN() throws JsonProcessingException {
        TransactionInProgress trx =
                TransactionInProgressFaker.mockInstance(7, SyncTrxStatus.CREATED);

        AuthPaymentResponseDTO responseDTO =
                AuthPaymentResponseDTOFaker.mockInstance(7,SyncTrxStatus.REJECTED);

        when(objectMapper.readValue(anyString(), ArgumentMatchers.eq(AuthPaymentResponseDTO.class))).thenReturn(responseDTO);

        AuthPaymentDTO preview =
                rewardCalculatorConnector.previewTransaction(trx);
        assertNotNull(preview);
    }
    @Test
    void testPreviewTransaction_GENERIC_ERROR() {
        TransactionInProgress trx =
                TransactionInProgressFaker.mockInstance(4, SyncTrxStatus.CREATED);

        RewardCalculatorInvocationException exception = assertThrows(RewardCalculatorInvocationException.class,
                () -> rewardCalculatorConnector.previewTransaction(trx));
        assertEquals(PaymentConstants.ExceptionCode.GENERIC_ERROR, exception.getCode());
    }

    @Test
    void testPreviewTransaction_TOO_MANY_REQUEST() {
        TransactionInProgress trx =
                TransactionInProgressFaker.mockInstance(5, SyncTrxStatus.CREATED);

        TooManyRequestsException exception = assertThrows(TooManyRequestsException.class,
                () -> rewardCalculatorConnector.previewTransaction(trx));
        assertEquals(PaymentConstants.ExceptionCode.TOO_MANY_REQUESTS, exception.getCode());

    }

    @Test
    void testAuthorizePaymentOk() {
        TransactionInProgress trx =
                TransactionInProgressFaker.mockInstance(11, SyncTrxStatus.IDENTIFIED);
        AuthPaymentDTO response =
                rewardCalculatorConnector.authorizePayment(trx);
        assertNotNull(response);
    }

    @Test
    void testCancelTransactionOk() {
        TransactionInProgress trx =
                TransactionInProgressFaker.mockInstance(21, SyncTrxStatus.AUTHORIZED);
        trx.setReward(100L);
        AuthPaymentDTO response =
                rewardCalculatorConnector.cancelTransaction(trx);
        log.info(String.valueOf(response));
        assertNotNull(response);
    }
    @Test
    void testCancelTransaction_404() {
        TransactionInProgress trx =
                TransactionInProgressFaker.mockInstance(22, SyncTrxStatus.CREATED);
        trx.setReward(100L);
        trx.setId("ID_CANCEL_NOT_FOUND");
        AuthPaymentDTO response =
                rewardCalculatorConnector.cancelTransaction(trx);
        assertNull(response);
    }

}