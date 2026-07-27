package it.gov.pagopa.payment.service.payment.expired;

import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.common.CommonConfirmServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QRCodeCancelExpiredServiceImplTest {

    @Mock
    private TransactionRepository transactionRepositoryMock;
    @Mock
    private AuditUtilities auditUtilitiesMock;
    @Mock
    private CommonConfirmServiceImpl commonConfirmServiceMock;

    private QRCodeCancelExpiredServiceImpl qrCodeCancelExpiredService;

    private static final long CANCEL_EXPIRATION_MINUTES = 15L;
    private static final String INITIATIVE_ID = "INITIATIVE_123";
    private static final String TRX_ID = "TRX_ID_123";

    @BeforeEach
    void setUp() {
        qrCodeCancelExpiredService = new QRCodeCancelExpiredServiceImpl(
                CANCEL_EXPIRATION_MINUTES,
                transactionRepositoryMock,
                auditUtilitiesMock,
                commonConfirmServiceMock
        );
    }

    @Test
    void testGetExpirationMinutesAndFlowName() {
        assertEquals(CANCEL_EXPIRATION_MINUTES, qrCodeCancelExpiredService.getExpirationMinutes());
        assertEquals("TRANSACTION_CANCEL_EXPIRED", qrCodeCancelExpiredService.getFlowName());
    }

    @Test
    void testFindExpiredTransaction_Success() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setId(TRX_ID);
        transaction.setStatus(SyncTrxStatus.AUTHORIZED);

        when(transactionRepositoryMock.findAndModifyExpiredTransaction(
                any(LocalDateTime.class),
                eq(List.of(SyncTrxStatus.AUTHORIZED.name())),
                eq(INITIATIVE_ID),
                eq(1000)
        )).thenReturn(Optional.of(transaction));

        // When
        Transaction result = qrCodeCancelExpiredService.findExpiredTransaction(INITIATIVE_ID, CANCEL_EXPIRATION_MINUTES);

        // Then
        assertNotNull(result);
        assertEquals(TRX_ID, result.getId());
        verify(transactionRepositoryMock, times(1)).findAndModifyExpiredTransaction(
                any(LocalDateTime.class),
                eq(List.of(SyncTrxStatus.AUTHORIZED.name())),
                eq(INITIATIVE_ID),
                eq(1000)
        );
    }

    @Test
    void testFindExpiredTransaction_NotFound_ThrowsException() {
        // Given
        when(transactionRepositoryMock.findAndModifyExpiredTransaction(
                any(LocalDateTime.class),
                eq(List.of(SyncTrxStatus.AUTHORIZED.name())),
                eq(INITIATIVE_ID),
                eq(1000)
        )).thenReturn(Optional.empty());

        // When & Then
        TransactionNotFoundOrExpiredException exception = assertThrows(
                TransactionNotFoundOrExpiredException.class,
                () -> qrCodeCancelExpiredService.findExpiredTransaction(INITIATIVE_ID, CANCEL_EXPIRATION_MINUTES)
        );

        assertTrue(exception.getMessage().contains("Cannot find transaction in findExpiredTransaction with initiativeId"));
    }

    @Test
    void testHandleExpiredTransaction_Success() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setId(TRX_ID);
        transaction.setStatus(SyncTrxStatus.AUTHORIZED);

        doNothing().when(commonConfirmServiceMock).confirmAuthorizedPayment(transaction);

        // When
        Transaction result = qrCodeCancelExpiredService.handleExpiredTransaction(transaction);

        // Then
        assertNotNull(result);
        assertEquals(TRX_ID, result.getId());
        verify(commonConfirmServiceMock, times(1)).confirmAuthorizedPayment(transaction);
    }
}