package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.connector.storage.FileStorageClient;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.RevertTransactionAuditDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.*;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonReversalServiceImplTest {

    @Mock
    private TransactionRepository transactionRepositoryMock;
    @Mock
    private FileStorageClient fileStorageClientMock;
    @Mock
    private AuditUtilities auditUtilitiesMock;
    @Mock
    private RewardBatchEligibilityPreflightService rewardBatchEligibilityPreflightServiceMock;
    @InjectMocks
    private CommonReversalServiceImpl commonReversalService;

    private static final String TRX_ID = "TRX_ID_123";
    private static final String MERCHANT_ID = "MERCHANT_ID_123";
    private static final String POS_ID = "POS_ID_123";
    private static final String DOC_NUMBER = "CREDIT_NOTE_12345";
    private static final String INITIATIVE_ID = "INITIATIVE_123";
    private static final String USER_ID = "USER_123";

    @Test
    void testReversalTransaction_Success(){
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "credit_note.pdf", "application/pdf", "content".getBytes());
        Transaction transaction = createDummyTransaction(SyncTrxStatus.CAPTURED, MERCHANT_ID, POS_ID);

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));
        // When
        commonReversalService.reversalTransaction(INITIATIVE_ID, TRX_ID, MERCHANT_ID, file, DOC_NUMBER);

        // Then
        assertEquals(SyncTrxStatus.REFUNDED, transaction.getStatus());
        assertNotNull(transaction.getCreditNoteData());
        assertEquals("credit_note.pdf", transaction.getCreditNoteData().getFilename());
        assertEquals(DOC_NUMBER, transaction.getCreditNoteData().getDocNumber());

        // MODIFICA: il path dello storage ora usa l'initiativeId invece del nome categoria "elettrodomestici".
        String expectedPath = String.format("invoices/%s/merchant/%s/pos/%s/transaction/%s/creditNote/%s",
                INITIATIVE_ID, MERCHANT_ID, POS_ID, TRX_ID, file.getOriginalFilename());
        verify(fileStorageClientMock, times(1)).upload(any(InputStream.class), eq(expectedPath), eq(file.getContentType()));
        verify(auditUtilitiesMock, times(1)).logReverseTransaction(any(RevertTransactionAuditDTO.class));
        verify(transactionRepositoryMock, times(1)).save(transaction);
        verify(auditUtilitiesMock, never()).logErrorReversalTransaction(any(), any());
    }

    @Test
    void testReversalTransaction_InvalidFileExtension() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "invalid_file.exe", "application/octet-stream", "content".getBytes());

        // When & Then
        InvalidInvoiceFormatException exception = assertThrows(
                InvalidInvoiceFormatException.class,
                () -> commonReversalService.reversalTransaction(INITIATIVE_ID, TRX_ID, MERCHANT_ID, file, DOC_NUMBER)
        );

        assertEquals("PAYMENT_GENERIC_ERROR", exception.getCode());
        verify(auditUtilitiesMock, times(1)).logErrorReversalTransaction(TRX_ID, MERCHANT_ID);
        verifyNoInteractions(transactionRepositoryMock, fileStorageClientMock);
    }

    @Test
    void testReversalTransaction_TransactionNotFound() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "credit_note.pdf", "application/pdf", "content".getBytes());
        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.empty());

        // When & Then
        TransactionNotFoundOrExpiredException exception = assertThrows(
                TransactionNotFoundOrExpiredException.class,
                () -> commonReversalService.reversalTransaction(INITIATIVE_ID, TRX_ID, MERCHANT_ID, file, DOC_NUMBER)
        );

        assertTrue(exception.getMessage().contains("Cannot find transaction with transactionId"));
        verify(auditUtilitiesMock, times(1)).logErrorReversalTransaction(TRX_ID, MERCHANT_ID);
    }

    @Test
    void testReversalTransaction_MerchantMismatch() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "credit_note.pdf", "application/pdf", "content".getBytes());
        Transaction transaction = createDummyTransaction(SyncTrxStatus.CAPTURED, "OTHER_MERCHANT", POS_ID);

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));

        // When & Then
        TransactionInvalidException exception = assertThrows(
                TransactionInvalidException.class,
                () -> commonReversalService.reversalTransaction(INITIATIVE_ID, TRX_ID, MERCHANT_ID, file, DOC_NUMBER)
        );

        assertEquals(ExceptionCode.GENERIC_ERROR, exception.getCode());
        assertTrue(exception.getMessage().contains("associated to the transaction is not equal to the merchant"));
        verify(auditUtilitiesMock, times(1)).logErrorReversalTransaction(TRX_ID, MERCHANT_ID);
    }

    @Test
    void testReversalTransaction_InitiativeMismatch() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "credit_note.pdf", "application/pdf", "content".getBytes());
        Transaction transaction = createDummyTransaction(SyncTrxStatus.CAPTURED, MERCHANT_ID, POS_ID);

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));

        // When & Then
        InitiativeNotfoundException exception = assertThrows(
                InitiativeNotfoundException.class,
                () -> commonReversalService.reversalTransaction("OTHER_INITIATIVE", TRX_ID, MERCHANT_ID, file, DOC_NUMBER)
        );

        assertTrue(exception.getMessage().contains("associated to the transaction is not equal to the initiative"));
        verify(auditUtilitiesMock, times(1)).logErrorReversalTransaction(TRX_ID, MERCHANT_ID);
    }

    @Test
    void testReversalTransaction_UsesPointOfSaleFromTransaction() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "credit_note.pdf", "application/pdf", "content".getBytes());
        String transactionPosId = "OTHER_POS";
        Transaction transaction = createDummyTransaction(SyncTrxStatus.CAPTURED, MERCHANT_ID, transactionPosId);

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));

        // When
        commonReversalService.reversalTransaction(INITIATIVE_ID, TRX_ID, MERCHANT_ID, file, DOC_NUMBER);

        // Then
        // MODIFICA: il path dello storage ora usa l'initiativeId invece del nome categoria "elettrodomestici".
        // Il codice di produzione (StoragePathUtils.buildCreditNotePath) costruisce il path con transaction.getInitiativeId().
        String expectedPath = String.format("invoices/%s/merchant/%s/pos/%s/transaction/%s/creditNote/%s",
                INITIATIVE_ID, MERCHANT_ID, transactionPosId, TRX_ID, file.getOriginalFilename());
        verify(fileStorageClientMock, times(1)).upload(any(InputStream.class), eq(expectedPath), eq(file.getContentType()));
        verify(transactionRepositoryMock, times(1)).save(transaction);
        verify(auditUtilitiesMock, never()).logErrorReversalTransaction(any(), any());
    }

    @Test
    void testReversalTransaction_InvalidStatus() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "credit_note.pdf", "application/pdf", "content".getBytes());
        Transaction transaction = createDummyTransaction(SyncTrxStatus.AUTHORIZED, MERCHANT_ID, POS_ID);

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));

        // When & Then
        OperationNotAllowedException exception = assertThrows(
                OperationNotAllowedException.class,
                () -> commonReversalService.reversalTransaction(INITIATIVE_ID, TRX_ID, MERCHANT_ID, file, DOC_NUMBER)
        );

        assertEquals(ExceptionCode.TRX_STATUS_NOT_VALID, exception.getCode());
        verify(auditUtilitiesMock, times(1)).logErrorReversalTransaction(TRX_ID, MERCHANT_ID);
    }

    @Test
    void testReversalTransaction_FileUploadIOException() throws Exception {
        // Given
        MultipartFile fileMock = mock(MultipartFile.class);
        when(fileMock.getOriginalFilename()).thenReturn("credit_note.pdf");
        when(fileMock.getInputStream()).thenThrow(new java.io.IOException("Disk upload error"));

        Transaction transaction = createDummyTransaction(SyncTrxStatus.CAPTURED, MERCHANT_ID, POS_ID);
        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));

        // When & Then
        InternalServerErrorException exception = assertThrows(
                InternalServerErrorException.class,
                () -> commonReversalService.reversalTransaction(INITIATIVE_ID, TRX_ID, MERCHANT_ID, fileMock, DOC_NUMBER)
        );

        assertEquals(ExceptionCode.GENERIC_ERROR, exception.getCode());
        assertEquals("Error uploading credit note file", exception.getMessage());
        verify(auditUtilitiesMock, times(1)).logErrorReversalTransaction(TRX_ID, MERCHANT_ID);
    }

    @Test
    void testReversalTransaction_EligibilityFailureDoesNotMutateBlobOrTransaction() {
        MockMultipartFile file = new MockMultipartFile("file", "credit_note.pdf", "application/pdf", "content".getBytes());
        Transaction transaction = createDummyTransaction(SyncTrxStatus.CAPTURED, MERCHANT_ID, POS_ID);
        String authorization = "Bearer token";

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));
        doThrow(new RewardBatchEligibilityNotAllowedException("Not allowed"))
                .when(rewardBatchEligibilityPreflightServiceMock)
                .verifyEligibility(transaction, MERCHANT_ID, authorization);

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> commonReversalService.reversalTransaction(
                        INITIATIVE_ID, TRX_ID, MERCHANT_ID, authorization, file, DOC_NUMBER));

        InOrder inOrder = inOrder(rewardBatchEligibilityPreflightServiceMock, fileStorageClientMock);
        inOrder.verify(rewardBatchEligibilityPreflightServiceMock)
                .verifyEligibility(transaction, MERCHANT_ID, authorization);
        verifyNoInteractions(fileStorageClientMock);
        verify(transactionRepositoryMock, never()).save(any());
    }


    private Transaction createDummyTransaction(SyncTrxStatus status, String merchantId, String pointOfSaleId) {
        Transaction transaction = new Transaction();
        transaction.setId(TRX_ID);
        transaction.setTrxCode("TRX_CODE_123");
        transaction.setInitiativeId(INITIATIVE_ID);
        transaction.setUserId(USER_ID);
        transaction.setMerchantId(merchantId);
        transaction.setPointOfSaleId(pointOfSaleId);
        transaction.setStatus(status);
        transaction.setRewardCents(150L);
        return transaction;
    }
}