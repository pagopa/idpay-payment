package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.merchant.dto.PointOfSaleDTO;
import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityOperation;
import it.gov.pagopa.payment.connector.storage.FileStorageClient;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.TransactionAuditDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.PointOfSaleTypeEnum;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.*;
import it.gov.pagopa.payment.model.InvoiceData;
import it.gov.pagopa.payment.repository.InvoiceTransactionCommand;
import it.gov.pagopa.payment.repository.InvoiceTransactionRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonInvoiceServiceImplTest {

    @Mock
    private TransactionRepository transactionRepositoryMock;
    @Mock
    private FileStorageClient fileStorageClientMock;
    @Mock
    private AuditUtilities auditUtilitiesMock;
    @Mock
    private MerchantConnector merchantConnectorMock;
    @Mock
    private RewardBatchEligibilityPreflightService rewardBatchEligibilityPreflightServiceMock;
    @Mock
    private InvoiceTransactionRepository invoiceTransactionRepositoryMock;

    private CommonInvoiceServiceImpl commonInvoiceService;

    private static final long MIN_DAYS_TO_INVOICE = 2L;
    private static final String TRX_ID = "TRX_ID_123";
    private static final String MERCHANT_ID = "MERCHANT_ID_123";
    private static final String POS_ID = "POS_ID_123";
    private static final String DOC_NUMBER = "DOC_12345";
    private static final String INITIATIVE_ID = "INITIATIVE_123";
    private static final String USER_ID = "USER_123";

    @BeforeEach
    void setUp() {
        commonInvoiceService = new CommonInvoiceServiceImpl(
                MIN_DAYS_TO_INVOICE,
                transactionRepositoryMock,
                fileStorageClientMock,
                auditUtilitiesMock,
                merchantConnectorMock,
                rewardBatchEligibilityPreflightServiceMock,
                invoiceTransactionRepositoryMock
        );
    }

    @Test
    void testInvoiceUpdateTransaction(){
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test_invoice.pdf", "application/pdf", "content".getBytes());
        Transaction transaction = createDummyTransaction(SyncTrxStatus.INVOICED, MERCHANT_ID, POS_ID);
        transaction.setElaborationDateTime(LocalDateTime.now(ZoneId.of("Europe/Rome")).minusDays(3));
        transaction.setInvoiceData(InvoiceData.builder().filename("filename").docNumber("123").build());
        PointOfSaleDTO posDTO = new PointOfSaleDTO();
        posDTO.setFranchiseName("Franchise Test");
        posDTO.setType(PointOfSaleTypeEnum.PHYSICAL);
        posDTO.setBusinessName("Business Test");
        posDTO.setFiscalCode("FISCAL_CODE_123");

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));

        // When
        commonInvoiceService.invoiceTransaction(INITIATIVE_ID, TRX_ID, MERCHANT_ID, file, DOC_NUMBER);

        // Then
        verify(rewardBatchEligibilityPreflightServiceMock).verifyEligibility(
                transaction,
                RewardBatchEligibilityOperation.INVOICE_REPLACEMENT,
                null);
        verify(auditUtilitiesMock).logInvoiceReplacement(any(TransactionAuditDTO.class));
    }

    @Test
    void testRewardedInvoiceReplacementChecksEligibilityAndRollsBackStatus() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test_invoice.pdf", "application/pdf", "content".getBytes());
        Transaction transaction = createDummyTransaction(SyncTrxStatus.REWARDED, MERCHANT_ID, POS_ID);
        transaction.setInvoiceData(InvoiceData.builder()
                .filename("old_invoice.pdf")
                .docNumber("OLD_DOC")
                .build());
        String authorization = "******";

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));

        commonInvoiceService.invoiceTransaction(
                INITIATIVE_ID, TRX_ID, MERCHANT_ID, authorization, file, DOC_NUMBER);

        verify(rewardBatchEligibilityPreflightServiceMock).verifyEligibility(
                transaction,
                RewardBatchEligibilityOperation.INVOICE_REPLACEMENT,
                authorization);
        verify(fileStorageClientMock).deleteFile(anyString());
        verify(fileStorageClientMock).upload(any(InputStream.class), anyString(), eq(file.getContentType()));
        verify(invoiceTransactionRepositoryMock).updateInvoiceAndCreateEvent(argThat(command ->
                command.transactionId().equals(TRX_ID)
                        && command.expectedStatus() == SyncTrxStatus.REWARDED
                        && command.expectedRevision() == 0
                        && command.invoiceData().getFilename().equals("test_invoice.pdf")
                        && command.eventType().name().equals("TRANSACTION_INVOICE_REPLACED")));
        verify(auditUtilitiesMock).logInvoiceReplacement(any(TransactionAuditDTO.class));
        assertEquals(SyncTrxStatus.REWARDED, transaction.getStatus());
        assertEquals("old_invoice.pdf", transaction.getInvoiceData().getFilename());
    }

    @Test
    void testInvoiceTransaction_Success_WithPosFetch(){
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test_invoice.pdf", "application/pdf", "content".getBytes());
        Transaction transaction = createDummyTransaction(SyncTrxStatus.CAPTURED, MERCHANT_ID, POS_ID);
        transaction.setElaborationDateTime(LocalDateTime.now(ZoneId.of("Europe/Rome")).minusDays(3));

        PointOfSaleDTO posDTO = new PointOfSaleDTO();
        posDTO.setFranchiseName("Franchise Test");
        posDTO.setType(PointOfSaleTypeEnum.PHYSICAL);
        posDTO.setBusinessName("Business Test");
        posDTO.setFiscalCode("FISCAL_CODE_123");

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));
        when(merchantConnectorMock.getPointOfSale(MERCHANT_ID, POS_ID)).thenReturn(posDTO);

        // When
        commonInvoiceService.invoiceTransaction(INITIATIVE_ID, TRX_ID, MERCHANT_ID, file, DOC_NUMBER);

        // Then
        // MODIFICA: il path dello storage ora usa l'initiativeId invece del nome categoria "elettrodomestici".
        String expectedPath = String.format("invoices/%s/merchant/%s/pos/%s/transaction/%s/invoice/%s",
                INITIATIVE_ID, MERCHANT_ID, POS_ID, TRX_ID, file.getOriginalFilename());
        verify(fileStorageClientMock, times(1)).upload(any(InputStream.class), eq(expectedPath), eq(file.getContentType()));
        verify(auditUtilitiesMock, times(1)).logInvoiceTransaction(any(TransactionAuditDTO.class));
        verify(invoiceTransactionRepositoryMock).updateInvoiceAndCreateEvent(argThat(command ->
                command.transactionId().equals(TRX_ID)
                        && command.expectedStatus() == SyncTrxStatus.CAPTURED
                        && command.expectedRevision() == 0
                        && command.franchiseName().equals("Franchise Test")
                        && command.pointOfSaleType().equals("PHYSICAL")
                        && command.eventType().name().equals("TRANSACTION_INVOICED")));
        verify(auditUtilitiesMock, never()).logErrorInvoiceTransaction(any(), any());
        verifyNoInteractions(rewardBatchEligibilityPreflightServiceMock);
    }

    @Test
    void testInvoiceTransaction_Success_PosDetailsAlreadyPresent() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test_invoice.pdf", "application/pdf", "content".getBytes());
        Transaction transaction = createDummyTransaction(SyncTrxStatus.CAPTURED, MERCHANT_ID, POS_ID);
        transaction.setElaborationDateTime(LocalDateTime.now(ZoneId.of("Europe/Rome")).minusDays(3));
        transaction.setFranchiseName("Already Existing Franchise");
        transaction.setPointOfSaleType("PHYSICAL");

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));

        // When
        commonInvoiceService.invoiceTransaction(INITIATIVE_ID, TRX_ID, MERCHANT_ID, file, DOC_NUMBER);

        // Then
        verify(merchantConnectorMock, never()).getPointOfSale(any(), any());
        verify(invoiceTransactionRepositoryMock).updateInvoiceAndCreateEvent(any(InvoiceTransactionCommand.class));
    }

    @Test
    void testInvoiceTransaction_InvalidFileExtension() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "invalid_file.exe", "application/octet-stream", "content".getBytes());

        // When & Then
        InvalidInvoiceFormatException exception = assertThrows(
                InvalidInvoiceFormatException.class,
                () -> commonInvoiceService.invoiceTransaction(INITIATIVE_ID, TRX_ID, MERCHANT_ID, file, DOC_NUMBER)
        );

        assertEquals("PAYMENT_GENERIC_ERROR", exception.getCode());
        verify(auditUtilitiesMock, times(1)).logErrorInvoiceTransaction(TRX_ID, MERCHANT_ID);
        verifyNoInteractions(transactionRepositoryMock, fileStorageClientMock);
    }

    @Test
    void testInvoiceTransaction_TransactionNotFound() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());
        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.empty());

        // When & Then
        TransactionNotFoundOrExpiredException exception = assertThrows(
                TransactionNotFoundOrExpiredException.class,
                () -> commonInvoiceService.invoiceTransaction(INITIATIVE_ID, TRX_ID, MERCHANT_ID, file, DOC_NUMBER)
        );

        assertTrue(exception.getMessage().contains("Cannot find transaction with transactionId"));
        verify(auditUtilitiesMock, times(1)).logErrorInvoiceTransaction(TRX_ID, MERCHANT_ID);
    }

    @Test
    void testInvoiceTransaction_MerchantMismatch() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());
        Transaction transaction = createDummyTransaction(SyncTrxStatus.CAPTURED, "OTHER_MERCHANT", POS_ID);

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));

        // When & Then
        TransactionInvalidException exception = assertThrows(
                TransactionInvalidException.class,
                () -> commonInvoiceService.invoiceTransaction(INITIATIVE_ID, TRX_ID, MERCHANT_ID, file, DOC_NUMBER)
        );

        assertEquals(ExceptionCode.GENERIC_ERROR, exception.getCode());
        assertTrue(exception.getMessage().contains("associated to the transaction is not equal to the merchant"));
        verify(auditUtilitiesMock, times(1)).logErrorInvoiceTransaction(TRX_ID, MERCHANT_ID);
    }

    @Test
    void testInvoiceTransaction_InitiativeMismatch() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());
        Transaction transaction = createDummyTransaction(SyncTrxStatus.CAPTURED, MERCHANT_ID, POS_ID);

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));

        // When & Then
        InitiativeNotfoundException exception = assertThrows(
                InitiativeNotfoundException.class,
                () -> commonInvoiceService.invoiceTransaction("OTHER_INITIATIVE", TRX_ID, MERCHANT_ID, file, DOC_NUMBER)
        );

        assertTrue(exception.getMessage().contains("associated to the transaction is not equal to the initiative"));
        verify(auditUtilitiesMock, times(1)).logErrorInvoiceTransaction(TRX_ID, MERCHANT_ID);
    }

    @Test
    void testInvoiceTransaction_UsesPointOfSaleFromTransaction() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());
        String transactionPosId = "OTHER_POS";
        Transaction transaction = createDummyTransaction(SyncTrxStatus.CAPTURED, MERCHANT_ID, transactionPosId);
        transaction.setElaborationDateTime(LocalDateTime.now(ZoneId.of("Europe/Rome")).minusDays(3));

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));
        PointOfSaleDTO posDTO = new PointOfSaleDTO();
        posDTO.setFranchiseName("Franchise Test");
        posDTO.setType(PointOfSaleTypeEnum.PHYSICAL);
        posDTO.setBusinessName("Business Test");
        posDTO.setFiscalCode("FISCAL_CODE_123");
        when(merchantConnectorMock.getPointOfSale(MERCHANT_ID, transactionPosId)).thenReturn(posDTO);

        // When
        commonInvoiceService.invoiceTransaction(INITIATIVE_ID, TRX_ID, MERCHANT_ID, file, DOC_NUMBER);

        // Then
        // MODIFICA: il path dello storage ora usa l'initiativeId invece del nome categoria "elettrodomestici".
        // Il codice di produzione (StoragePathUtils.buildInvoicePath) costruisce il path con transaction.getInitiativeId().
        String expectedPath = String.format("invoices/%s/merchant/%s/pos/%s/transaction/%s/invoice/%s",
                INITIATIVE_ID, MERCHANT_ID, transactionPosId, TRX_ID, file.getOriginalFilename());
        verify(fileStorageClientMock, times(1)).upload(any(InputStream.class), eq(expectedPath), eq(file.getContentType()));
        verify(invoiceTransactionRepositoryMock).updateInvoiceAndCreateEvent(any(InvoiceTransactionCommand.class));
        verify(auditUtilitiesMock, never()).logErrorInvoiceTransaction(any(), any());
    }

    @Test
    void testInvoiceTransaction_InvalidStatus() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());
        Transaction transaction = createDummyTransaction(SyncTrxStatus.AUTHORIZED, MERCHANT_ID, POS_ID);

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));

        // When & Then
        OperationNotAllowedException exception = assertThrows(
                OperationNotAllowedException.class,
                () -> commonInvoiceService.invoiceTransaction(INITIATIVE_ID, TRX_ID, MERCHANT_ID, file, DOC_NUMBER)
        );

        assertEquals(ExceptionCode.TRX_STATUS_NOT_VALID, exception.getCode());
        verify(auditUtilitiesMock, times(1)).logErrorInvoiceTransaction(TRX_ID, MERCHANT_ID);
    }

    @Test
    void testInvoiceTransaction_RewardedWithoutInvoiceIsInvalid() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());
        Transaction transaction = createDummyTransaction(SyncTrxStatus.REWARDED, MERCHANT_ID, POS_ID);

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));

        OperationNotAllowedException exception = assertThrows(
                OperationNotAllowedException.class,
                () -> commonInvoiceService.invoiceTransaction(
                        INITIATIVE_ID, TRX_ID, MERCHANT_ID, file, DOC_NUMBER));

        assertEquals(ExceptionCode.TRX_STATUS_NOT_VALID, exception.getCode());
        verifyNoInteractions(rewardBatchEligibilityPreflightServiceMock, fileStorageClientMock);
        verifyNoInteractions(invoiceTransactionRepositoryMock);
    }

    @Test
    void testInvoiceTransaction_TransactionTooRecent() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());
        Transaction transaction = createDummyTransaction(SyncTrxStatus.CAPTURED, MERCHANT_ID, POS_ID);
        transaction.setElaborationDateTime(LocalDateTime.now(ZoneId.of("Europe/Rome"))); // Creata adesso, minDaysToInvoice è 2

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));

        // When & Then
        OperationNotAllowedException exception = assertThrows(
                OperationNotAllowedException.class,
                () -> commonInvoiceService.invoiceTransaction(INITIATIVE_ID, TRX_ID, MERCHANT_ID, file, DOC_NUMBER)
        );

        assertEquals(ExceptionCode.TRX_TOO_RECENT, exception.getCode());
        verify(auditUtilitiesMock, times(1)).logErrorInvoiceTransaction(TRX_ID, MERCHANT_ID);
    }

    @Test
    void testInvoiceTransaction_FileUploadIOException() throws Exception {
        // Given
        MultipartFile fileMock = mock(MultipartFile.class);
        when(fileMock.getOriginalFilename()).thenReturn("test.pdf");
        when(fileMock.getInputStream()).thenThrow(new java.io.IOException("Disk error"));

        Transaction transaction = createDummyTransaction(SyncTrxStatus.CAPTURED, MERCHANT_ID, POS_ID);
        transaction.setElaborationDateTime(LocalDateTime.now(ZoneId.of("Europe/Rome")).minusDays(3));

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));

        // When & Then
        InternalServerErrorException exception = assertThrows(
                InternalServerErrorException.class,
                () -> commonInvoiceService.invoiceTransaction(INITIATIVE_ID, TRX_ID, MERCHANT_ID, fileMock, DOC_NUMBER)
        );

        assertEquals(ExceptionCode.GENERIC_ERROR, exception.getCode());
        assertEquals("Error uploading invoice file", exception.getMessage());
        verify(auditUtilitiesMock, times(1)).logErrorInvoiceTransaction(TRX_ID, MERCHANT_ID);
    }

    @Test
    void testInvoiceTransaction_EligibilityFailureDoesNotMutateBlobOrTransaction() {
        MockMultipartFile file = new MockMultipartFile("file", "test_invoice.pdf", "application/pdf", "content".getBytes());
        Transaction transaction = createDummyTransaction(SyncTrxStatus.INVOICED, MERCHANT_ID, POS_ID);
        transaction.setElaborationDateTime(LocalDateTime.now(ZoneId.of("Europe/Rome")).minusDays(3));
        InvoiceData originalInvoiceData = InvoiceData.builder()
                .filename("old_invoice.pdf")
                .docNumber("OLD_DOC")
                .build();
        transaction.setInvoiceData(originalInvoiceData);
        transaction.setTransactionRevision(3L);
        String authorization = "Bearer token";

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));
        doThrow(new RewardBatchEligibilityNotAllowedException("Not allowed"))
                .when(rewardBatchEligibilityPreflightServiceMock)
                .verifyEligibility(
                        transaction,
                        RewardBatchEligibilityOperation.INVOICE_REPLACEMENT,
                        authorization);

        assertThrows(
                RewardBatchEligibilityNotAllowedException.class,
                () -> commonInvoiceService.invoiceTransaction(
                        INITIATIVE_ID, TRX_ID, MERCHANT_ID, authorization, file, DOC_NUMBER));

        InOrder inOrder = inOrder(rewardBatchEligibilityPreflightServiceMock, fileStorageClientMock);
        inOrder.verify(rewardBatchEligibilityPreflightServiceMock)
                .verifyEligibility(
                        transaction,
                        RewardBatchEligibilityOperation.INVOICE_REPLACEMENT,
                        authorization);
        verifyNoInteractions(fileStorageClientMock, merchantConnectorMock);
        verifyNoInteractions(invoiceTransactionRepositoryMock);
        verify(auditUtilitiesMock, never()).logInvoiceTransaction(any());
        assertEquals(SyncTrxStatus.INVOICED, transaction.getStatus());
        assertSame(originalInvoiceData, transaction.getInvoiceData());
        assertEquals(3L, transaction.getTransactionRevision());
    }

    @Test
    void testInvoiceTransaction_ConcurrentChangeRaisesConflictAndDoesNotAuditSuccess() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test_invoice.pdf", "application/pdf", "content".getBytes());
        Transaction transaction = createDummyTransaction(SyncTrxStatus.INVOICED, MERCHANT_ID, POS_ID);
        transaction.setInvoiceData(InvoiceData.builder()
                .filename("old_invoice.pdf")
                .docNumber("OLD_DOC")
                .build());
        transaction.setTransactionRevision(4L);

        when(transactionRepositoryMock.findById(TRX_ID)).thenReturn(Optional.of(transaction));
        doThrow(new TransactionConflictException(
                ExceptionCode.TRANSACTION_CONFLICT,
                "Concurrent change"))
                .when(invoiceTransactionRepositoryMock)
                .updateInvoiceAndCreateEvent(any(InvoiceTransactionCommand.class));

        TransactionConflictException exception = assertThrows(
                TransactionConflictException.class,
                () -> commonInvoiceService.invoiceTransaction(
                        INITIATIVE_ID, TRX_ID, MERCHANT_ID, file, DOC_NUMBER));

        assertEquals(ExceptionCode.TRANSACTION_CONFLICT, exception.getCode());
        verify(auditUtilitiesMock, never()).logInvoiceTransaction(any());
        verify(auditUtilitiesMock, never()).logInvoiceReplacement(any());
        verify(auditUtilitiesMock).logErrorInvoiceTransaction(TRX_ID, MERCHANT_ID);
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
        transaction.setRewardCents(200L);
        transaction.setTransactionRevision(0L);
        return transaction;
    }
}