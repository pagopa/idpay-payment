package it.gov.pagopa.payment.service.payment.qrcode;

import static org.mockito.Mockito.when;

import it.gov.pagopa.common.web.exception.custom.badrequest.OperationNotAllowedException;
import it.gov.pagopa.common.web.exception.custom.forbidden.UserNotAllowedException;
import it.gov.pagopa.common.web.exception.custom.notfound.TransactionNotFoundOrExpiredException;
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
                        rewardCalculatorConnectorMock,
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
            Assertions.assertEquals("[UNRELATE_TRANSACTION] Cannot find transaction having code: TRXCODE", e.getMessage());

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
            Assertions.assertEquals(ExceptionCode.PAYMENT_USER_NOT_VALID, e.getCode());
            Assertions.assertEquals("[UNRELATE_TRANSACTION] Requesting userId (USERID) not allowed to operate on transaction having id MOCKEDTRANSACTION_qr-code_0", e.getMessage());
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
            Assertions.assertEquals(ExceptionCode.TRX_STATUS_NOT_VALID, e.getCode());
            Assertions.assertEquals("[UNRELATE_TRANSACTION] Cannot unrelate transaction not in status IDENTIFIED: id MOCKEDTRANSACTION_qr-code_0", e.getMessage());
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
    void testSuccessfulRewardCalculator404() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstanceBuilder(0, SyncTrxStatus.IDENTIFIED)
                .userId(USERID)
                .build();
        when(repositoryMockFindInvocation()).thenReturn(trx);

        when(rewardCalculatorConnectorMock.cancelTransaction(trx)).thenThrow(new TransactionNotFoundOrExpiredException("NOT_FOUND", "msg"));

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