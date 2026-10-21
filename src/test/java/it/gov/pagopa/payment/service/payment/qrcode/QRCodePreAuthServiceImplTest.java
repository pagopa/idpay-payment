package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QRCodePreAuthServiceImplTest {

    @Mock
    private TransactionRepository transactionRepositoryMock;
    @Mock
    private RewardCalculatorConnector rewardCalculatorConnectorMock;
    @Mock
    private AuditUtilities auditUtilitiesMock;
    @Mock
    private WalletConnector walletConnectorMock;

    private QRCodePreAuthServiceImpl qrCodePreAuthService;

    private static final long AUTHORIZATION_EXPIRATION_MINUTES = 15L;
    private static final String TRX_CODE = "TRX_CODE_123";
    private static final String LOWER_TRX_CODE = "trx_code_123";
    private static final String USER_ID = "USER_ID_123";
    private static final String INITIATIVE_ID = "INITIATIVE_ID_123";

    @BeforeEach
    void setUp() {
        qrCodePreAuthService = spy(new QRCodePreAuthServiceImpl(
                AUTHORIZATION_EXPIRATION_MINUTES,
                transactionRepositoryMock,
                rewardCalculatorConnectorMock,
                auditUtilitiesMock,
                walletConnectorMock
        ));
    }

    @Test
    void testRelateUser_Success() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setId("TRX_ID_123");
        transaction.setTrxCode(LOWER_TRX_CODE);
        transaction.setInitiativeId(INITIATIVE_ID);

        AuthPaymentDTO expectedAuthPaymentDTO = new AuthPaymentDTO();
        expectedAuthPaymentDTO.setStatus(SyncTrxStatus.IDENTIFIED);

        when(transactionRepositoryMock.findByTrxCode(LOWER_TRX_CODE))
                .thenReturn(Optional.of(transaction));

        doReturn(transaction).when(qrCodePreAuthService).relateUser(transaction, USER_ID);
        doReturn(expectedAuthPaymentDTO).when(qrCodePreAuthService)
                .previewPayment(transaction, RewardConstants.TRX_CHANNEL_QRCODE, SyncTrxStatus.IDENTIFIED);
        doNothing().when(qrCodePreAuthService).auditLogRelateUser(transaction, RewardConstants.TRX_CHANNEL_QRCODE);

        // When
        AuthPaymentDTO result = qrCodePreAuthService.relateUser(TRX_CODE, USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(SyncTrxStatus.IDENTIFIED, result.getStatus());

        verify(transactionRepositoryMock, times(1)).findByTrxCode(LOWER_TRX_CODE);
        verify(qrCodePreAuthService, times(1)).relateUser(transaction, USER_ID);
        verify(qrCodePreAuthService, times(1)).previewPayment(transaction, RewardConstants.TRX_CHANNEL_QRCODE, SyncTrxStatus.IDENTIFIED);
        verify(qrCodePreAuthService, times(1)).auditLogRelateUser(transaction, RewardConstants.TRX_CHANNEL_QRCODE);
    }

    @Test
    void testRelateUser_TransactionNotFound_ThrowsException() {
        // Given
        when(transactionRepositoryMock.findByTrxCode(LOWER_TRX_CODE))
                .thenReturn(Optional.empty());

        // When & Then
        TransactionNotFoundOrExpiredException exception = assertThrows(
                TransactionNotFoundOrExpiredException.class,
                () -> qrCodePreAuthService.relateUser(TRX_CODE, USER_ID)
        );

        assertTrue(exception.getMessage().contains("Cannot find transaction with trxCode"));

        verify(transactionRepositoryMock, times(1)).findByTrxCode(LOWER_TRX_CODE);
        verify(qrCodePreAuthService, never()).relateUser(any(Transaction.class), any());
        verify(qrCodePreAuthService, never()).previewPayment(any(), any(), any());
        verify(qrCodePreAuthService, never()).auditLogRelateUser(any(), any());
    }
}