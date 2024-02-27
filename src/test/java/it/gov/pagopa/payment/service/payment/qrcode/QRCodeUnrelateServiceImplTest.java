package it.gov.pagopa.payment.service.payment.qrcode;

import static org.mockito.Mockito.when;

import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.UserNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
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
                        auditUtilitiesMock);
    }

    @Test
    void testTrxNotFound() {
        when(repositoryMockFindInvocation()).thenReturn(null);

        try {
            invokeService();
            Assertions.fail("Expected exception");
        } catch (TransactionNotFoundOrExpiredException e) {
            Assertions.assertEquals(ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, e.getCode());
            Assertions.assertEquals("Cannot find transaction with trxCode [TRXCODE]", e.getMessage());

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
        } catch (UserNotAllowedException e) {
            Assertions.assertEquals(ExceptionCode.TRX_ALREADY_ASSIGNED, e.getCode());
            Assertions.assertEquals("Transaction with trxCode [TRXCODE] is already assigned to another user", e.getMessage());
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
        } catch (OperationNotAllowedException e) {
            Assertions.assertEquals(ExceptionCode.TRX_UNRELATE_NOT_ALLOWED, e.getCode());
            Assertions.assertEquals("Cannot unrelate transaction with transactionId [MOCKEDTRANSACTION_qr-code_0] not in status identified", e.getMessage());
        }
    }

    @Test
    void testSuccessful() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstanceBuilder(0, SyncTrxStatus.IDENTIFIED)
                .userId(USERID)
                .build();
        when(repositoryMockFindInvocation()).thenReturn(trx);

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