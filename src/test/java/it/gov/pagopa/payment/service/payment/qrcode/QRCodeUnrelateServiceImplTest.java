package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.common.utils.TransactionSynchronizer;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.exception.custom.UserNotAllowedException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.expired.QRCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QRCodeUnrelateServiceImplTest {
    public static final String TRXCODE = "TRXCODE";
    public static final String USERID = "USERID";

    @Mock private TransactionRepository transactionRepository;
    @Mock private TransactionInProgressRepository repositoryMock;
    @Mock private QRCodeAuthorizationExpiredService qrCodeAuthorizationExpiredServiceMock;
    @Mock private RewardCalculatorConnector rewardCalculatorConnectorMock;
    @Mock private AuditUtilities auditUtilitiesMock;
    @Mock private TransactionSynchronizer transactionSynchronizer;

    private QRCodeUnrelateService service;

    @BeforeEach
    void init() {
        service =
                new QRCodeUnrelateServiceImpl(
                        transactionRepository,
                        repositoryMock,
                        qrCodeAuthorizationExpiredServiceMock,
                        auditUtilitiesMock,
                        transactionSynchronizer);
    }

    @Test
    void testTrxNotFound() {
        when(repositoryMockFindInvocation()).thenReturn(null);

        TransactionNotFoundOrExpiredException exception = Assertions.assertThrows(
                TransactionNotFoundOrExpiredException.class,
                this::invokeService
        );

        Assertions.assertEquals(ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, exception.getCode());
        Assertions.assertEquals("Cannot find transaction with trxCode [TRXCODE]", exception.getMessage());
    }

    @Test
    void testUserIdForbidden() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);

        when(transactionRepository.findByTrxCodeAndAuthorizationNotExpired(anyString(), any())).thenReturn(Optional.of(transaction));
        when(repositoryMockFindInvocation())
                .thenReturn(TransactionInProgressFaker.mockInstanceBuilder(0, SyncTrxStatus.IDENTIFIED)
                        .userId(USERID + "1")
                        .build()
                );

        UserNotAllowedException exception = Assertions.assertThrows(
                UserNotAllowedException.class,
                this::invokeService
        );

        Assertions.assertEquals(ExceptionCode.TRX_ALREADY_ASSIGNED, exception.getCode());
        Assertions.assertEquals("Transaction with trxCode [TRXCODE] is already assigned to another user", exception.getMessage());
    }

    @Test
    void testExpiredTransaction() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);

        when(transactionRepository.findByTrxCodeAndAuthorizationNotExpired(anyString(), any())).thenReturn(Optional.of(transaction));
        when(repositoryMockFindInvocation()).thenReturn(
                TransactionInProgressFaker.mockInstanceBuilder(0, SyncTrxStatus.AUTHORIZED)
                        .userId(USERID).build());

        OperationNotAllowedException exception = Assertions.assertThrows(
                OperationNotAllowedException.class,
                this::invokeService
        );

        Assertions.assertEquals(ExceptionCode.TRX_UNRELATE_NOT_ALLOWED, exception.getCode());
        Assertions.assertEquals("Cannot unrelate transaction with transactionId [MOCKEDTRANSACTION_qr-code_0] not in status identified", exception.getMessage());
    }

    @Test
    void testSuccessful() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        when(transactionRepository.findByTrxCodeAndAuthorizationNotExpired(anyString(),any())).thenReturn(Optional.of(transaction));
        TransactionInProgress trx = TransactionInProgressFaker.mockInstanceBuilder(0, SyncTrxStatus.IDENTIFIED)
                .userId(USERID)
                .build();
        when(repositoryMockFindInvocation()).thenReturn(trx);

        invokeService();

        TransactionInProgress expectedTrx = trx.toBuilder().status(SyncTrxStatus.CREATED).userId(null).build();

        verify(repositoryMock).save(expectedTrx);
    }

    private TransactionInProgress repositoryMockFindInvocation() {
        return qrCodeAuthorizationExpiredServiceMock.findByTrxCodeAndAuthorizationNotExpired(TRXCODE.toLowerCase());
    }

    private void invokeService() {
        service.unrelateTransaction(TRXCODE, USERID);
    }
}