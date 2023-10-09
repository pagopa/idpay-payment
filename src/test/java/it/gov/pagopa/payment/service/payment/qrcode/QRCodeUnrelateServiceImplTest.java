package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.expired.QRCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QRCodeUnrelateServiceImplTest {
    public static final String TRXCODE = "TRXCODE";
    public static final String USERID = "USERID";

    @Mock private TransactionInProgressRepository repositoryMock;
    @Mock private QRCodeAuthorizationExpiredService qrCodeAuthorizationExpiredServiceMock;
    @Mock private RewardCalculatorConnector rewardCalculatorConnectorMock;
    @Mock private AuditUtilities auditUtilitiesMock;

    private QRCodeUnrelateService service;

    @BeforeEach
    void init() {
        service =
                new QRCodeUnrelateServiceImpl(
                        repositoryMock,
                        qrCodeAuthorizationExpiredServiceMock,
                        rewardCalculatorConnectorMock,
                        auditUtilitiesMock);
    }

    @Test
    void testTrxNotFound() {
        when(repositoryMockFindInvocation()).thenReturn(null);

        try {
            invokeService();
            Assertions.fail("Expected exception");
        } catch (ClientExceptionNoBody e) {
            Assertions.assertEquals(HttpStatus.NOT_FOUND, e.getHttpStatus());
        }
    }

    @Test
    void testUserIdForbidden() {
        when(repositoryMockFindInvocation())
                .thenReturn(TransactionInProgressFaker.mockInstanceBuilder(0, SyncTrxStatus.IDENTIFIED)
                        .userId(USERID+"1")
                        .build()
                );

        try {
            invokeService();
            Assertions.fail("Expected exception");
        } catch (ClientExceptionNoBody e) {
            Assertions.assertEquals(HttpStatus.FORBIDDEN, e.getHttpStatus());
        }
    }

    @Test
    void testExpiredTransaction() {
        when(repositoryMockFindInvocation()).thenReturn(
                TransactionInProgressFaker.mockInstanceBuilder(0, SyncTrxStatus.AUTHORIZED)
                        .userId(USERID).build());

        try {
            invokeService();
            Assertions.fail("Expected exception");
        } catch (ClientExceptionNoBody e) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus());
        }
    }

    @Test
    void testSuccessful() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstanceBuilder(0, SyncTrxStatus.IDENTIFIED)
                .userId(USERID)
                .build();
        when(repositoryMockFindInvocation()).thenReturn(trx);

        when(rewardCalculatorConnectorMock.cancelTransaction(trx)).thenReturn(new AuthPaymentDTO());

        invokeService();

        TransactionInProgress expectedTrx = trx.toBuilder().status(SyncTrxStatus.CREATED).userId(null).build();

        Mockito.verify(repositoryMock).save(expectedTrx);
    }

    @Test
    void testSuccessfulRewardCalculator403() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstanceBuilder(0, SyncTrxStatus.IDENTIFIED)
                .userId(USERID)
                .build();
        when(repositoryMockFindInvocation()).thenReturn(trx);

        when(rewardCalculatorConnectorMock.cancelTransaction(trx)).thenThrow(new ClientExceptionNoBody(HttpStatus.FORBIDDEN, "msg"));

        invokeService();

        TransactionInProgress expectedTrx = trx.toBuilder().status(SyncTrxStatus.CREATED).userId(null).build();

        Mockito.verify(repositoryMock).save(expectedTrx);
    }

    @Test
    void testSuccessfulRewardCalculator404() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstanceBuilder(0, SyncTrxStatus.IDENTIFIED)
                .userId(USERID)
                .build();
        when(repositoryMockFindInvocation()).thenReturn(trx);

        when(rewardCalculatorConnectorMock.cancelTransaction(trx)).thenThrow(new ClientExceptionNoBody(HttpStatus.NOT_FOUND, "msg"));

        invokeService();

        TransactionInProgress expectedTrx = trx.toBuilder().status(SyncTrxStatus.CREATED).userId(null).build();

        Mockito.verify(repositoryMock).save(expectedTrx);
    }

    private TransactionInProgress repositoryMockFindInvocation() {
        return qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(TRXCODE.toLowerCase());
    }

    private void invokeService() {
        service.unrelateTransaction(TRXCODE, USERID);
    }
}