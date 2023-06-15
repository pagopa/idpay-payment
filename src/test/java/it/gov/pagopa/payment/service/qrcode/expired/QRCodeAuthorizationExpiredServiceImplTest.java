package it.gov.pagopa.payment.service.qrcode.expired;

import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class QRCodeAuthorizationExpiredServiceImplTest {

    @Mock private TransactionInProgressRepository transactionInProgressRepositoryMock;
    @Mock private RewardCalculatorConnector rewardCalculatorConnectorMock;
    private final AuditUtilities auditUtilities = new AuditUtilities();
    private QRCodeAuthorizationExpiredService qrCodeAuthorizationExpiredService;

    @BeforeEach
    void setUp() {
        qrCodeAuthorizationExpiredService = new QRCodeAuthorizationExpiredServiceImpl(transactionInProgressRepositoryMock, rewardCalculatorConnectorMock, auditUtilities);
    }

    @Test
    void handleExpiredTransaction() {
        TransactionInProgress trxCreate = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        TransactionInProgress trxIdentified = TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.IDENTIFIED);
        TransactionInProgress trxIdentifiedException404 = TransactionInProgressFaker.mockInstance(3, SyncTrxStatus.IDENTIFIED);
        TransactionInProgress trxIdentifiedException500 = TransactionInProgressFaker.mockInstance(4, SyncTrxStatus.IDENTIFIED);

        Mockito.when(transactionInProgressRepositoryMock.findAuthorizationExpiredTransactionThrottled())
                .thenReturn(trxCreate)
                .thenReturn(trxIdentified)
                .thenReturn(trxIdentifiedException404)
                .thenReturn(trxIdentifiedException500)
                .thenReturn(null);


        AuthPaymentDTO authTrx = AuthPaymentDTOFaker.mockInstance(1, trxIdentified);
        Mockito.when(rewardCalculatorConnectorMock.cancelTransaction(trxIdentified)).thenReturn(authTrx);

        Mockito.when(rewardCalculatorConnectorMock.cancelTransaction(trxIdentifiedException404)).thenThrow(new ClientExceptionNoBody(HttpStatus.NOT_FOUND, "NOT_FOUND"));
        Mockito.when(rewardCalculatorConnectorMock.cancelTransaction(trxIdentifiedException500)).thenThrow(new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR"));

        qrCodeAuthorizationExpiredService.execute();
        Mockito.verify(transactionInProgressRepositoryMock).deleteById(trxCreate.getId());
        Mockito.verify(transactionInProgressRepositoryMock).deleteById(trxIdentified.getId());
        Mockito.verify(transactionInProgressRepositoryMock).deleteById(trxIdentifiedException404.getId());
    }

    @Test
    void handleExpiredTransactionException() {
        TransactionInProgress trxIdentified = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);

        Mockito.when(transactionInProgressRepositoryMock.findAuthorizationExpiredTransactionThrottled())
                .thenReturn(trxIdentified)
                .thenReturn(null);


        Mockito.when(rewardCalculatorConnectorMock.cancelTransaction(trxIdentified)).thenThrow(new RuntimeException());

        qrCodeAuthorizationExpiredService.execute();
        Mockito.verify(transactionInProgressRepositoryMock, Mockito.never()).deleteById(Mockito.any());
    }
}