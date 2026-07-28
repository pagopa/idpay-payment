package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BarCodeCaptureServiceImplTest {

    @Mock
    private TransactionRepository transactionRepositoryMock;
    @Mock
    private TransactionMapper transactionMapperMock;
    @Mock
    private AuditUtilities auditUtilitiesMock;
    @Mock
    private MerchantConnector merchantConnector;
    @InjectMocks
    private BarCodeCaptureServiceImpl barCodeCaptureService;

    private static final String TRX_CODE = "TRX_CODE_123";
    private static final String LOWER_TRX_CODE = "trx_code_123";
    private static final String INITIATIVE_ID = "INITIATIVE_ID";
    private static final String USER_ID = "USER_ID";
    private static final String TRX_ID = "TRX_ID";
    private static final String MERCHANT_ID = "MERCHANT_ID";
    private static final String POS_ID = "POS_ID";
    private static final String ACQUIRER_ID = "ACQUIRER_ID";

    @Test
    void testCapturePayment_Success() {
        // Given
        Transaction transaction = createDummyTransaction(SyncTrxStatus.AUTHORIZED);

        Transaction unusedTrx = new Transaction();
        unusedTrx.setId("UNUSED_TRX_ID");
        unusedTrx.setExtendedAuthorization(true);

        List<Transaction> unusedList = List.of(unusedTrx);

        TransactionBarCodeResponse expectedResponse = new TransactionBarCodeResponse();

        when(transactionRepositoryMock.findByTrxCodeAndStatusNot(LOWER_TRX_CODE, SyncTrxStatus.CANCELLED)).thenReturn(Optional.of(transaction));
        when(transactionRepositoryMock.findByUserIdAndInitiativeIdAndStatusAndExtendedAuthorizationNot(
                USER_ID, INITIATIVE_ID, SyncTrxStatus.CREATED, false
        )).thenReturn(unusedList);
        when(merchantConnector.merchantDetail(MERCHANT_ID, INITIATIVE_ID)).thenReturn(null);
        when(merchantConnector.getPointOfSale(MERCHANT_ID, POS_ID, INITIATIVE_ID)).thenReturn(null);
        when(transactionMapperMock.transactionBarCodeToTransactionResponse(transaction)).thenReturn(expectedResponse);

        // When
        TransactionBarCodeResponse result = barCodeCaptureService.capturePayment(INITIATIVE_ID, TRX_CODE, MERCHANT_ID, POS_ID, ACQUIRER_ID);

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        assertEquals(SyncTrxStatus.CAPTURED, transaction.getStatus());
        assertNotNull(transaction.getElaborationDateTime());
        assertNotNull(transaction.getUpdateDate());

        verify(transactionRepositoryMock, times(1)).findByTrxCodeAndStatusNot(LOWER_TRX_CODE, SyncTrxStatus.CANCELLED);
        verify(transactionRepositoryMock, times(1)).deleteAll(unusedList);
        verify(transactionRepositoryMock, times(1)).save(transaction);
        verify(auditUtilitiesMock, times(1)).logCapturePayment(
                INITIATIVE_ID, TRX_ID, LOWER_TRX_CODE, USER_ID, 100L, null, MERCHANT_ID
        );
        verify(auditUtilitiesMock, never()).logErrorCapturePayment(any());
    }

    @Test
    void testCapturePayment_Success_NoUnusedVouchers() {
        // Given
        Transaction transaction = createDummyTransaction(SyncTrxStatus.AUTHORIZED);
        TransactionBarCodeResponse expectedResponse = new TransactionBarCodeResponse();

        when(transactionRepositoryMock.findByTrxCodeAndStatusNot(LOWER_TRX_CODE, SyncTrxStatus.CANCELLED)).thenReturn(Optional.of(transaction));
        when(transactionRepositoryMock.findByUserIdAndInitiativeIdAndStatusAndExtendedAuthorizationNot(
                USER_ID, INITIATIVE_ID, SyncTrxStatus.CREATED, false
        )).thenReturn(Collections.emptyList());
        when(merchantConnector.merchantDetail(MERCHANT_ID, INITIATIVE_ID)).thenReturn(null);
        when(merchantConnector.getPointOfSale(MERCHANT_ID, POS_ID, INITIATIVE_ID)).thenReturn(null);
        when(transactionMapperMock.transactionBarCodeToTransactionResponse(transaction)).thenReturn(expectedResponse);

        // When
        TransactionBarCodeResponse result = barCodeCaptureService.capturePayment(INITIATIVE_ID, TRX_CODE, MERCHANT_ID, POS_ID, ACQUIRER_ID);

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(transactionRepositoryMock, never()).deleteAll(anyList());
        verify(transactionRepositoryMock, times(1)).save(transaction);
    }

    @Test
    void testCapturePayment_NotFound() {
        // Given
        when(transactionRepositoryMock.findByTrxCodeAndStatusNot(LOWER_TRX_CODE, SyncTrxStatus.CANCELLED)).thenReturn(Optional.empty());

        // When & Then
        TransactionNotFoundOrExpiredException exception = assertThrows(
                TransactionNotFoundOrExpiredException.class,
                () -> barCodeCaptureService.capturePayment(INITIATIVE_ID, TRX_CODE, MERCHANT_ID, POS_ID, ACQUIRER_ID)
        );

        assertEquals("Cannot find transaction with transactionCode [%s]".formatted(TRX_CODE), exception.getMessage());
        verify(auditUtilitiesMock, times(1)).logErrorCapturePayment(TRX_CODE);
        verify(transactionRepositoryMock, never()).save(any());
    }

    @Test
    void testCapturePayment_InvalidStatus() {
        // Given
        Transaction transaction = createDummyTransaction(SyncTrxStatus.CREATED);
        when(transactionRepositoryMock.findByTrxCodeAndStatusNot(LOWER_TRX_CODE, SyncTrxStatus.CANCELLED)).thenReturn(Optional.of(transaction));

        // When & Then
        OperationNotAllowedException exception = assertThrows(
                OperationNotAllowedException.class,
                () -> barCodeCaptureService.capturePayment(INITIATIVE_ID, TRX_CODE, MERCHANT_ID, POS_ID, ACQUIRER_ID)
        );

        assertEquals(PaymentConstants.ExceptionCode.TRX_OPERATION_NOT_ALLOWED, exception.getCode());
        assertTrue(exception.getMessage().contains("Cannot operate on transaction with transactionCode"));
        verify(auditUtilitiesMock, times(1)).logErrorCapturePayment(TRX_CODE);
        verify(transactionRepositoryMock, never()).save(any());
    }

    @Test
    void testRetriveVoucher_Success() {
        // Given
        Transaction transaction = createDummyTransaction(SyncTrxStatus.CREATED);
        TransactionBarCodeResponse expectedResponse = new TransactionBarCodeResponse();

        when(transactionRepositoryMock.findByInitiativeIdAndTrxCodeAndUserId(INITIATIVE_ID, TRX_CODE, USER_ID))
                .thenReturn(Optional.of(transaction));
        when(transactionMapperMock.transactionBarCodeToTransactionResponse(transaction))
                .thenReturn(expectedResponse);

        // When
        TransactionBarCodeResponse result = barCodeCaptureService.retriveVoucher(INITIATIVE_ID, TRX_CODE, USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(auditUtilitiesMock, times(1)).logRetriveVoucher(
                INITIATIVE_ID, TRX_ID, LOWER_TRX_CODE, USER_ID, 100L, null
        );
        verify(auditUtilitiesMock, never()).logErrorRetriveVoucher(any(), any(), any());
    }

    @Test
    void testRetriveVoucher_NotFound() {
        // Given
        when(transactionRepositoryMock.findByInitiativeIdAndTrxCodeAndUserId(INITIATIVE_ID, TRX_CODE, USER_ID))
                .thenReturn(Optional.empty());

        // When & Then
        TransactionNotFoundOrExpiredException exception = assertThrows(
                TransactionNotFoundOrExpiredException.class,
                () -> barCodeCaptureService.retriveVoucher(INITIATIVE_ID, TRX_CODE, USER_ID)
        );

        assertEquals("Cannot find voucher with transactionCode [%s]".formatted(TRX_CODE), exception.getMessage());
        verify(auditUtilitiesMock, times(1)).logErrorRetriveVoucher(INITIATIVE_ID, TRX_CODE, USER_ID);
    }

    private Transaction createDummyTransaction(SyncTrxStatus status) {
        Transaction transaction = new Transaction();
        transaction.setId(TRX_ID);
        transaction.setTrxCode(LOWER_TRX_CODE);
        transaction.setInitiativeId(INITIATIVE_ID);
        transaction.setUserId(USER_ID);
        transaction.setMerchantId(MERCHANT_ID);
        transaction.setPointOfSaleId(POS_ID);
        transaction.setAcquirerId(ACQUIRER_ID);
        transaction.setStatus(status);
        transaction.setRewardCents(100L);
        transaction.setExtendedAuthorization(false);
        return transaction;
    }
}