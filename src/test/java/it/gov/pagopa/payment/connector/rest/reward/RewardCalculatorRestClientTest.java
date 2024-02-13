package it.gov.pagopa.payment.connector.rest.reward;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import it.gov.pagopa.payment.configuration.FeignConfig;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.connector.rest.reward.mapper.RewardCalculatorMapper;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.RewardCalculatorInvocationException;
import it.gov.pagopa.payment.exception.custom.TooManyRequestsException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.AuthPaymentResponseDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(
        initializers = RewardCalculatorRestClientTest.WireMockInitializer.class,
        classes = {
                RewardCalculatorConnectorImpl.class,
                FeignAutoConfiguration.class,
                FeignConfig.class,
                HttpMessageConvertersAutoConfiguration.class,
                RewardCalculatorMapper.class
        })
@TestPropertySource(
        locations = "classpath:application.yml",
        properties = "spring.application.name=idpay-reward-calculator")
@Slf4j
class RewardCalculatorRestClientTest {

    @Autowired
    private RewardCalculatorRestClient rewardCalculatorRestClient;

    @Autowired
    private RewardCalculatorConnector rewardCalculatorConnector;

    @MockBean
    ObjectMapper objectMapper;
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
                TransactionInProgressFaker.mockInstance(11, SyncTrxStatus.CREATED);
        AuthPaymentDTO response =
                rewardCalculatorConnector.authorizePayment(trx);
        assertNotNull(response);
    }

    @Test
    void testCancelTransactionOk() {
        TransactionInProgress trx =
                TransactionInProgressFaker.mockInstance(21, SyncTrxStatus.CREATED);
        AuthPaymentDTO response =
                rewardCalculatorConnector.cancelTransaction(trx);
        assertNotNull(response);
    }
    @Test
    void testCancelTransaction_404() {
        TransactionInProgress trx =
                TransactionInProgressFaker.mockInstance(22, SyncTrxStatus.CREATED);
        AuthPaymentDTO response =
                rewardCalculatorConnector.cancelTransaction(trx);
        assertNull(response);
    }

    public static class WireMockInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            WireMockServer wireMockServer = new WireMockServer(new WireMockConfiguration().dynamicPort());
            wireMockServer.start();

            applicationContext.getBeanFactory().registerSingleton("wireMockServer", wireMockServer);

            applicationContext.addApplicationListener(
                    applicationEvent -> {
                        if (applicationEvent instanceof ContextClosedEvent) {
                            wireMockServer.stop();
                        }
                    });

            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    String.format(
                            "rest-client.reward.baseUrl=http://%s:%d",
                            wireMockServer.getOptions().bindAddress(), wireMockServer.port()));
        }
    }
}
