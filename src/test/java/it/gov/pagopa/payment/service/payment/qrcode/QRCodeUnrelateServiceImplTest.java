package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.exception.custom.UserNotAllowedException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QRCodeUnrelateServiceImplTest {

    @Mock
    private TransactionRepository transactionRepositoryMock;
    @Mock
    private AuditUtilities auditUtilitiesMock;
    @InjectMocks
    private QRCodeUnrelateServiceImpl qrCodeUnrelateService;

    private static final String TRX_ID = "TRX_ID_123";
    private static final String TRX_CODE = "TRX_CODE_123";
    private static final String USER_ID = "USER_ID_123";
    private static final String INITIATIVE_ID = "INITIATIVE_ID_123";

    @Test
    void testUnrelateTransaction_Success() {
        // Given
        Transaction transaction = createDummyTransaction(SyncTrxStatus.IDENTIFIED, USER_ID);
        transaction.setRewardCents(500L);
        transaction.setChannel(RewardConstants.TRX_CHANNEL_QRCODE);
        transaction.setRejectionReasons(List.of("REASON_1"));

        when(transactionRepositoryMock.findByTrxCodeAndTrxEndDateGreaterThanEqual(eq(TRX_CODE), any(OffsetDateTime.class)))
                .thenReturn(Optional.of(transaction));
        when(transactionRepositoryMock.save(transaction)).thenReturn(transaction);

        // When
        qrCodeUnrelateService.unrelateTransaction(TRX_CODE, USER_ID);

        // Then
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryMock, times(1)).save(transactionCaptor.capture());

        Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(SyncTrxStatus.CREATED, savedTransaction.getStatus());
        assertNull(savedTransaction.getUserId());
        assertNull(savedTransaction.getRewardCents());
        assertNull(savedTransaction.getRewards());
        assertNull(savedTransaction.getChannel());
        assertNull(savedTransaction.getTrxChargeDate());
        assertEquals(Collections.emptyList(), savedTransaction.getRejectionReasons());

        verify(auditUtilitiesMock, times(1)).logUnrelateTransaction(
                INITIATIVE_ID, TRX_ID, TRX_CODE, null, 0L, List.of()
        );
        verify(auditUtilitiesMock, never()).logErrorUnrelateTransaction(any(), any());
    }

    @Test
    void testUnrelateTransaction_UserNotAllowed_ThrowsUserNotAllowedException() {
        // Given
        Transaction transaction = createDummyTransaction(SyncTrxStatus.IDENTIFIED, "OTHER_USER_ID");

        when(transactionRepositoryMock.findByTrxCodeAndTrxEndDateGreaterThanEqual(eq(TRX_CODE), any(OffsetDateTime.class)))
                .thenReturn(Optional.of(transaction));

        // When & Then
        UserNotAllowedException exception = assertThrows(
                UserNotAllowedException.class,
                () -> qrCodeUnrelateService.unrelateTransaction(TRX_CODE, USER_ID)
        );

        assertEquals(ExceptionCode.TRX_ALREADY_ASSIGNED, exception.getCode());
        assertTrue(exception.getMessage().contains("is already assigned to another user"));

        verify(transactionRepositoryMock, never()).save(any());
        verify(auditUtilitiesMock, times(1)).logErrorUnrelateTransaction(TRX_CODE, USER_ID);
        verify(auditUtilitiesMock, never()).logUnrelateTransaction(any(), any(), any(), any(), anyLong(), any());
    }

    @Test
    void testUnrelateTransaction_StatusNotIdentified_ThrowsOperationNotAllowedException() {
        // Given
        Transaction transaction = createDummyTransaction(SyncTrxStatus.CREATED, USER_ID);

        when(transactionRepositoryMock.findByTrxCodeAndTrxEndDateGreaterThanEqual(eq(TRX_CODE), any(OffsetDateTime.class)))
                .thenReturn(Optional.of(transaction));

        // When & Then
        OperationNotAllowedException exception = assertThrows(
                OperationNotAllowedException.class,
                () -> qrCodeUnrelateService.unrelateTransaction(TRX_CODE, USER_ID)
        );

        assertEquals(ExceptionCode.TRX_UNRELATE_NOT_ALLOWED, exception.getCode());
        assertTrue(exception.getMessage().contains("not in status identified"));

        verify(transactionRepositoryMock, never()).save(any());
        verify(auditUtilitiesMock, times(1)).logErrorUnrelateTransaction(TRX_CODE, USER_ID);
        verify(auditUtilitiesMock, never()).logUnrelateTransaction(any(), any(), any(), any(), anyLong(), any());
    }

    @Test
    void testUnrelateTransaction_TransactionNotFound_ThrowsTransactionNotFoundOrExpiredException() {
        // Given
        when(transactionRepositoryMock.findByTrxCodeAndTrxEndDateGreaterThanEqual(eq(TRX_CODE), any(OffsetDateTime.class)))
                .thenReturn(Optional.empty());

        // When & Then
        TransactionNotFoundOrExpiredException exception = assertThrows(
                TransactionNotFoundOrExpiredException.class,
                () -> qrCodeUnrelateService.unrelateTransaction(TRX_CODE, USER_ID)
        );

        assertTrue(exception.getMessage().contains("Cannot find transaction with trxCode"));

        verify(transactionRepositoryMock, never()).save(any());
        verify(auditUtilitiesMock, times(1)).logErrorUnrelateTransaction(TRX_CODE, USER_ID);
        verify(auditUtilitiesMock, never()).logUnrelateTransaction(any(), any(), any(), any(), anyLong(), any());
    }

    private Transaction createDummyTransaction(SyncTrxStatus status, String userId) {
        Transaction transaction = new Transaction();
        transaction.setId(TRX_ID);
        transaction.setTrxCode(TRX_CODE);
        transaction.setInitiativeId(INITIATIVE_ID);
        transaction.setUserId(userId);
        transaction.setStatus(status);
        return transaction;
    }
}