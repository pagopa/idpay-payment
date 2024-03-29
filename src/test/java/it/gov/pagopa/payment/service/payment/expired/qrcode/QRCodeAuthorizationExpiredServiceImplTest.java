package it.gov.pagopa.payment.service.payment.expired.qrcode;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.RewardCalculatorInvocationException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.expired.QRCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.service.payment.expired.QRCodeAuthorizationExpiredServiceImpl;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QRCodeAuthorizationExpiredServiceImplTest {

    private final static long EXPIRATION_MINUTES=15;

    @Mock private TransactionInProgressRepository transactionInProgressRepositoryMock;
    @Mock private RewardCalculatorConnector rewardCalculatorConnectorMock;

    private final AuditUtilities auditUtilities = new AuditUtilities();

    private QRCodeAuthorizationExpiredService qrCodeAuthorizationExpiredService;

    @BeforeEach
    void setUp() {
        qrCodeAuthorizationExpiredService = new QRCodeAuthorizationExpiredServiceImpl(EXPIRATION_MINUTES, transactionInProgressRepositoryMock, rewardCalculatorConnectorMock, auditUtilities);
    }

    @Test
    void findByTrxCodeAndAuthorizationNotExpired(){

        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        transaction.setUserId("USERID1");

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);

        when(transactionInProgressRepositoryMock.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode(),EXPIRATION_MINUTES))
                .thenReturn(transaction);
        qrCodeAuthorizationExpiredService.findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode());

        Mockito.verify(transactionInProgressRepositoryMock).findByTrxCodeAndAuthorizationNotExpired(transaction.getTrxCode(),EXPIRATION_MINUTES);

    }

    @Test
    void handleExpiredTransaction() {
        TransactionInProgress trxCreate = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        TransactionInProgress trxIdentified = TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.IDENTIFIED);
        TransactionInProgress trxIdentifiedException404 = TransactionInProgressFaker.mockInstance(3, SyncTrxStatus.IDENTIFIED);
        TransactionInProgress trxIdentifiedException500 = TransactionInProgressFaker.mockInstance(4, SyncTrxStatus.IDENTIFIED);

        Mockito.when(transactionInProgressRepositoryMock.findAuthorizationExpiredTransaction(null, EXPIRATION_MINUTES))
                .thenReturn(trxCreate)
                .thenReturn(trxIdentified)
                .thenReturn(trxIdentifiedException404)
                .thenReturn(trxIdentifiedException500)
                .thenReturn(null);


        AuthPaymentDTO authTrx = AuthPaymentDTOFaker.mockInstance(1, trxIdentified);
        Mockito.when(rewardCalculatorConnectorMock.cancelTransaction(trxIdentified)).thenReturn(authTrx);

        Mockito.when(rewardCalculatorConnectorMock.cancelTransaction(trxIdentifiedException404)).thenThrow(new TransactionNotFoundOrExpiredException("NOT_FOUND"));
        Mockito.when(rewardCalculatorConnectorMock.cancelTransaction(trxIdentifiedException500)).thenThrow(new RewardCalculatorInvocationException("INTERNAL_SERVER_ERROR"));

        qrCodeAuthorizationExpiredService.execute();
        Mockito.verify(transactionInProgressRepositoryMock).deleteById(trxCreate.getId());
        Mockito.verify(transactionInProgressRepositoryMock).deleteById(trxIdentified.getId());
        Mockito.verify(transactionInProgressRepositoryMock).deleteById(trxIdentifiedException404.getId());
    }

    @Test
    void handleExpiredTransactionException() {
        TransactionInProgress trxIdentified = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);

        Mockito.when(transactionInProgressRepositoryMock.findAuthorizationExpiredTransaction(null, EXPIRATION_MINUTES))
                .thenReturn(trxIdentified)
                .thenReturn(null);


        Mockito.when(rewardCalculatorConnectorMock.cancelTransaction(trxIdentified)).thenThrow(new RuntimeException());

        qrCodeAuthorizationExpiredService.execute();
        Mockito.verify(transactionInProgressRepositoryMock, Mockito.never()).deleteById(Mockito.any());
    }
}