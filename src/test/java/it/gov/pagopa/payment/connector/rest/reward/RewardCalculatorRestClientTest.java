package it.gov.pagopa.payment.connector.rest.reward;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import it.gov.pagopa.payment.configuration.FeignConfig;
import it.gov.pagopa.payment.connector.rest.reward.mapper.RewardCalculatorMapper;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TransactionVersionMismatchException;
import it.gov.pagopa.payment.exception.custom.TransactionVersionPendingException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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
        locations = "classpath:application.yml",
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

        TransactionVersionMismatchException exception = Assert.assertThrows(TransactionVersionMismatchException.class, () -> rewardCalculatorConnector.authorizePayment(trx));
        Assertions.assertEquals(PaymentConstants.ExceptionCode.PAYMENT_TRANSACTION_VERSION_MISMATCH,exception.getCode());
        Assertions.assertEquals("The transaction version mismatch",exception.getMessage() );
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

}