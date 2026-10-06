package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.dto.mapper.TransactionMapper;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonStatusTransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepositoryMock;
    @Mock
    private TransactionMapper transactionMapperMock;
    @InjectMocks
    private CommonStatusTransactionServiceImpl commonStatusTransactionService;

    private static final String TRANSACTION_ID = "TRX_ID_123";
    private static final String MERCHANT_ID = "MERCHANT_ID_123";
    private static final String WRONG_MERCHANT_ID = "WRONG_MERCHANT_ID_999";

    @Test
    void testGetStatusTransaction_Success() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setId(TRANSACTION_ID);
        transaction.setMerchantId(MERCHANT_ID);

        SyncTrxStatusDTO expectedStatus = new SyncTrxStatusDTO();

        when(transactionRepositoryMock.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));
        when(transactionMapperMock.transactionToSyncTrxStatus(transaction)).thenReturn(expectedStatus);

        // When
        SyncTrxStatusDTO result = commonStatusTransactionService.getStatusTransaction(TRANSACTION_ID, MERCHANT_ID);

        // Then
        assertNotNull(result);
        assertEquals(expectedStatus, result);
        verify(transactionRepositoryMock, times(1)).findById(TRANSACTION_ID);
        verify(transactionMapperMock, times(1)).transactionToSyncTrxStatus(transaction);
    }

    @Test
    void testGetStatusTransaction_NotFound() {
        // Given
        when(transactionRepositoryMock.findById(TRANSACTION_ID)).thenReturn(Optional.empty());

        // When & Then
        TransactionNotFoundOrExpiredException exception = assertThrows(
                TransactionNotFoundOrExpiredException.class,
                () -> commonStatusTransactionService.getStatusTransaction(TRANSACTION_ID, MERCHANT_ID)
        );

        assertEquals("Cannot find transaction with transactionId [%s]".formatted(TRANSACTION_ID), exception.getMessage());
        verify(transactionRepositoryMock, times(1)).findById(TRANSACTION_ID);
        verifyNoInteractions(transactionMapperMock);
    }

    @Test
    void testGetStatusTransaction_UnauthorizedMerchant() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setId(TRANSACTION_ID);
        transaction.setMerchantId(MERCHANT_ID);

        when(transactionRepositoryMock.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));

        // When & Then
        TransactionNotFoundOrExpiredException exception = assertThrows(
                TransactionNotFoundOrExpiredException.class,
                () -> commonStatusTransactionService.getStatusTransaction(TRANSACTION_ID, WRONG_MERCHANT_ID)
        );

        assertEquals("Cannot find transaction with transactionId [%s]".formatted(TRANSACTION_ID), exception.getMessage());
        verify(transactionRepositoryMock, times(1)).findById(TRANSACTION_ID);
        verifyNoInteractions(transactionMapperMock);
    }
}