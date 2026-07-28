package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.mapper.TransactionMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonConfirmServiceImplTest {

    @Mock
    private TransactionRepository transactionRepositoryMock;
    @Mock
    private TransactionMapper transactionMapperMock;
    @Mock
    private TransactionNotifierService notifierServiceMock;
    @Mock
    private PaymentErrorNotifierService paymentErrorNotifierServiceMock;
    @Mock
    private AuditUtilities auditUtilitiesMock;
    @InjectMocks
    private CommonConfirmServiceImpl commonConfirmService;

    private static final String TRX_ID = "TRX_ID_123";
    private static final String MERCHANT_ID = "MERCHANT_ID_123";
    private static final String ACQUIRER_ID = "ACQUIRER_ID_123";
    private static final String WRONG_MERCHANT_ID = "WRONG_MERCHANT";
    private static final String INITIATIVE_ID = "INITIATIVE_123";
    private static final String USER_ID = "USER_123";


    @Test
    void testConfirmPayment_Success() {
        // Given
        Transaction transaction = createDummyTransaction(SyncTrxStatus.AUTHORIZED, MERCHANT_ID, ACQUIRER_ID);
        TransactionResponse expectedResponse = new TransactionResponse();

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));
        when(notifierServiceMock.notify(transaction, MERCHANT_ID)).thenReturn(true);
        when(transactionMapperMock.transactionToTransactionResponse(transaction)).thenReturn(expectedResponse);

        // When
        TransactionResponse result = commonConfirmService.confirmPayment(TRX_ID, MERCHANT_ID, ACQUIRER_ID);

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        assertEquals(SyncTrxStatus.REWARDED, transaction.getStatus());
        assertNotNull(transaction.getElaborationDateTime());

        verify(transactionRepositoryMock, times(1)).save(transaction);
        verify(notifierServiceMock, times(1)).notify(transaction, MERCHANT_ID);
        verify(auditUtilitiesMock, times(1)).logConfirmedPayment(
                INITIATIVE_ID, TRX_ID, transaction.getTrxCode(), USER_ID, 100L, null, MERCHANT_ID
        );
        verify(auditUtilitiesMock, never()).logErrorConfirmedPayment(any(), any());
    }

    @Test
    void testConfirmPayment_NotFound() {
        // Given
        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.empty());

        // When & Then
        TransactionNotFoundOrExpiredException exception = assertThrows(
                TransactionNotFoundOrExpiredException.class,
                () -> commonConfirmService.confirmPayment(TRX_ID, MERCHANT_ID, ACQUIRER_ID)
        );

        assertEquals("Cannot find transaction with transactionId [%s]".formatted(TRX_ID), exception.getMessage());
        verify(auditUtilitiesMock, times(1)).logErrorConfirmedPayment(TRX_ID, MERCHANT_ID);
        verify(transactionRepositoryMock, never()).save(any());
    }

    @Test
    void testConfirmPayment_InvalidStatus() {
        // Given
        Transaction transaction = createDummyTransaction(SyncTrxStatus.CREATED, MERCHANT_ID, ACQUIRER_ID);
        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));

        // When & Then
        OperationNotAllowedException exception = assertThrows(
                OperationNotAllowedException.class,
                () -> commonConfirmService.confirmPayment(TRX_ID, MERCHANT_ID, ACQUIRER_ID)
        );

        assertEquals(PaymentConstants.ExceptionCode.TRX_OPERATION_NOT_ALLOWED, exception.getCode());
        verify(auditUtilitiesMock, times(1)).logErrorConfirmedPayment(TRX_ID, MERCHANT_ID);
        verify(transactionRepositoryMock, never()).save(any());
    }

    @Test
    void testConfirmPayment_MerchantOrAcquirerNotAllowed() {
        // Given
        Transaction transaction = createDummyTransaction(SyncTrxStatus.AUTHORIZED, MERCHANT_ID, ACQUIRER_ID);
        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));

        // When & Then (Wrong Merchant)
        MerchantOrAcquirerNotAllowedException exception = assertThrows(
                MerchantOrAcquirerNotAllowedException.class,
                () -> commonConfirmService.confirmPayment(TRX_ID, WRONG_MERCHANT_ID, ACQUIRER_ID)
        );

        assertTrue(exception.getMessage().contains("is not equal to the merchant with id"));
        verify(auditUtilitiesMock, times(1)).logErrorConfirmedPayment(TRX_ID, WRONG_MERCHANT_ID);
        verify(transactionRepositoryMock, never()).save(any());
    }

    @Test
    void testConfirmPayment_NotificationReturnsFalse_HandledByErrorNotifier() {
        // Given
        Transaction transaction = createDummyTransaction(SyncTrxStatus.AUTHORIZED, MERCHANT_ID, ACQUIRER_ID);
        TransactionResponse expectedResponse = new TransactionResponse();
        Message<Transaction> dummyMessage = MessageBuilder.withPayload(transaction).build();

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));
        when(notifierServiceMock.notify(transaction, MERCHANT_ID)).thenReturn(false);
        when(notifierServiceMock.buildMessage(transaction, MERCHANT_ID)).thenReturn(dummyMessage);
        when(paymentErrorNotifierServiceMock.notifyConfirmPayment(eq(dummyMessage), anyString(), eq(true), any()))
                .thenReturn(true);
        when(transactionMapperMock.transactionToTransactionResponse(transaction)).thenReturn(expectedResponse);

        // When
        TransactionResponse result = commonConfirmService.confirmPayment(TRX_ID, MERCHANT_ID, ACQUIRER_ID);

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(paymentErrorNotifierServiceMock, times(1))
                .notifyConfirmPayment(eq(dummyMessage), anyString(), eq(true), any());
        verify(transactionRepositoryMock, times(1)).save(transaction);
    }

    @Test
    void testConfirmPayment_NotificationThrowsException_ErrorNotifierFails() {
        // Given
        Transaction transaction = createDummyTransaction(SyncTrxStatus.AUTHORIZED, MERCHANT_ID, ACQUIRER_ID);
        TransactionResponse expectedResponse = new TransactionResponse();

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));
        when(notifierServiceMock.notify(transaction, MERCHANT_ID)).thenThrow(new RuntimeException("Kafka error"));
        when(notifierServiceMock.buildMessage(transaction, MERCHANT_ID)).thenReturn(null);
        when(paymentErrorNotifierServiceMock.notifyConfirmPayment(any(), anyString(), eq(true), any()))
                .thenReturn(false);
        when(transactionMapperMock.transactionToTransactionResponse(transaction)).thenReturn(expectedResponse);

        // When
        TransactionResponse result = commonConfirmService.confirmPayment(TRX_ID, MERCHANT_ID, ACQUIRER_ID);

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(paymentErrorNotifierServiceMock, times(1))
                .notifyConfirmPayment(any(), anyString(), eq(true), any());
        verify(transactionRepositoryMock, times(1)).save(transaction);
    }

    private Transaction createDummyTransaction(SyncTrxStatus status, String merchantId, String acquirerId) {
        Transaction transaction = new Transaction();
        transaction.setId(TRX_ID);
        transaction.setTrxCode("TRX_CODE_123");
        transaction.setInitiativeId(INITIATIVE_ID);
        transaction.setUserId(USER_ID);
        transaction.setMerchantId(merchantId);
        transaction.setAcquirerId(acquirerId);
        transaction.setStatus(status);
        transaction.setRewardCents(100L);
        return transaction;
    }
}