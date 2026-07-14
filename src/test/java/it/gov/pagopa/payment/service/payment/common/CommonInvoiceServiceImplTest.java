package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.utils.TransactionSynchronizer;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.merchant.dto.PointOfSaleDTO;
import it.gov.pagopa.payment.connector.storage.FileStorageClient;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.PointOfSaleTypeEnum;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InvalidInvoiceFormatException;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommonInvoiceServiceImplTest {
    @Mock
    private TransactionInProgressRepository repository;
    @Mock
    private TransactionNotifierService notifierService;
    @Mock
    private PaymentErrorNotifierService paymentErrorNotifierService;
    @Mock
    private FileStorageClient fileStorageClient;
    @Mock
    private AuditUtilities auditUtilities;
    @Mock
    private MultipartFile file;
    @Mock
    private MerchantConnector merchantConnector;
    @Mock private TransactionRepository transactionRepository;
    @Mock private TransactionSynchronizer transactionSynchronizer;

    private CommonInvoiceServiceImpl service;

    private static final String TRANSACTION_ID = "trxId";
    private static final String MERCHANT_ID = "merchantId";
    private static final String POS_ID = "posId";
    private static final String FILENAME = "invoice.pdf";
    private static final String DOCUMENT_NUMBER = "FPR 192/25";

    private TransactionInProgress trx;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        when(file.getOriginalFilename()).thenReturn(FILENAME);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        when(file.getContentType()).thenReturn("application/pdf");

        trx = TransactionInProgress.builder()
                .id(TRANSACTION_ID)
                .merchantId(MERCHANT_ID)
                .pointOfSaleId(POS_ID)
                .status(SyncTrxStatus.CAPTURED)
                .initiativeId("initId")
                .trxCode("trxCode")
                .userId("userId")
                .rewardCents(100L)
                .build();

        service = new CommonInvoiceServiceImpl(
                0,
                transactionRepository,
                repository,
                notifierService,
                paymentErrorNotifierService,
                fileStorageClient,
                auditUtilities,
                transactionSynchronizer,
                merchantConnector
        );
    }

    @Test
    void invoiceTransaction_success() {
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));

        PointOfSaleDTO pos = PointOfSaleDTO.builder()
            .franchiseName("Test")
            .type(PointOfSaleTypeEnum.PHYSICAL)
            .businessName("BUSINESS_NAME")
            .fiscalCode("FISCAL_CODE")
            .build();
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        when(merchantConnector.getPointOfSale(MERCHANT_ID, POS_ID))
            .thenReturn(pos);

        when(notifierService.notify(any(TransactionInProgress.class), anyString())).thenReturn(true);
        service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, DOCUMENT_NUMBER);
        verify(fileStorageClient).upload(any(), anyString(), anyString());
        verify(repository).save(trx);
        verify(auditUtilities).logInvoiceTransaction(any());
        assertEquals(SyncTrxStatus.INVOICED, trx.getStatus());
        assertEquals(FILENAME, trx.getInvoiceData().getFilename());
        assertEquals(DOCUMENT_NUMBER, trx.getInvoiceData().getDocNumber());
    }

    @Test
    void invoiceTransaction_transactionNotFound() {
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());
        assertThrows(TransactionNotFoundOrExpiredException.class,
                () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, DOCUMENT_NUMBER));
        verify(auditUtilities).logErrorInvoiceTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void invoiceTransaction_merchantMismatch() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        trx.setMerchantId("otherMerchant");
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        assertThrows(TransactionInvalidException.class,
                () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, DOCUMENT_NUMBER));
        verify(auditUtilities).logErrorInvoiceTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void invoiceTransaction_posMismatch() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        trx.setPointOfSaleId("otherPos");
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        assertThrows(TransactionInvalidException.class,
                () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, DOCUMENT_NUMBER));
        verify(auditUtilities).logErrorInvoiceTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void invoiceTransaction_statusNotCaptured() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        trx.setStatus(SyncTrxStatus.CREATED);
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        assertThrows(OperationNotAllowedException.class,
                () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, DOCUMENT_NUMBER));
        verify(auditUtilities).logErrorInvoiceTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void invoiceTransaction_invalidFileFormat_shouldThrowInvalidInvoiceFormatException() {
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        MultipartFile invalidFile = mock(MultipartFile.class);
        when(invalidFile.getOriginalFilename()).thenReturn("document.txt");
        InvalidInvoiceFormatException ex = assertThrows(InvalidInvoiceFormatException.class,
                () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, invalidFile, DOCUMENT_NUMBER));
        assertEquals("File must be a PDF or XML", ex.getMessage());
    }

    @Test
    void invoiceTransaction_nullFile_shouldThrowInvalidInvoiceFormatException() {
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        InvalidInvoiceFormatException ex = assertThrows(InvalidInvoiceFormatException.class,
                () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, null, DOCUMENT_NUMBER));
        assertEquals("File is required", ex.getMessage());
    }

    @Test
    void invoiceTransaction_invalidFileExtension_shouldThrowInvalidInvoiceFormatException() {
        MultipartFile invalidFile = new MockMultipartFile("file", "invoice.txt", "text/plain", "dummy".getBytes());
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));

        InvalidInvoiceFormatException ex = assertThrows(InvalidInvoiceFormatException.class,
            () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, invalidFile, DOCUMENT_NUMBER));
        assertEquals("File must be a PDF or XML", ex.getMessage());
    }


    @Test
    void invoiceTransaction_nullFileName_shouldThrowInvalidInvoiceFormatException() {
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        MultipartFile fileWithNullName = mock(MultipartFile.class);
        when(fileWithNullName.getOriginalFilename()).thenReturn(null);
        InvalidInvoiceFormatException ex = assertThrows(InvalidInvoiceFormatException.class,
                () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, fileWithNullName, DOCUMENT_NUMBER));
        assertEquals("File must be a PDF or XML", ex.getMessage());
    }

    @Test
    void invoiceTransaction_runtimeException_shouldLogAndThrow() {
        when(repository.findById(TRANSACTION_ID)).thenThrow(new RuntimeException("Generic error"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, DOCUMENT_NUMBER));
        assertEquals("Generic error", ex.getMessage());
        verify(auditUtilities).logErrorInvoiceTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void invoiceTransaction_ioException_shouldLogAndThrow() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        doThrow(new RuntimeException(new IOException("IO error"))).when(fileStorageClient).upload(any(), anyString(), anyString());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, DOCUMENT_NUMBER));
        assertEquals("IO error", ex.getCause().getMessage());
        verify(auditUtilities).logErrorInvoiceTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void sendInvoiceTransactionNotification_notifyReturnsFalse_shouldThrowInternalServerErrorException() {
        when(notifierService.notify(any(TransactionInProgress.class), anyString())).thenReturn(false);
        assertThrows(TransactionNotFoundOrExpiredException.class,
                () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, DOCUMENT_NUMBER));
    }

    @Test
    void invoiceTransaction_shouldSetCorrectInvoicePath() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));

        PointOfSaleDTO pos = PointOfSaleDTO.builder()
            .franchiseName("Franchise Test")
            .type(PointOfSaleTypeEnum.PHYSICAL)
            .build();

        when(merchantConnector.getPointOfSale(MERCHANT_ID, POS_ID)).thenReturn(pos);
        when(notifierService.notify(any(TransactionInProgress.class), anyString())).thenReturn(true);
        service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, DOCUMENT_NUMBER);
        String expectedPath = String.format("invoices/merchant/%s/pos/%s/transaction/%s/invoice/%s",
                MERCHANT_ID, POS_ID, trx.getId(), FILENAME);
        verify(fileStorageClient).upload(any(), eq(expectedPath), anyString());
    }

    @Test
    void shouldThrowOperationNotAllowedException_whenTrxIsTooRecent() {
        service = new CommonInvoiceServiceImpl(
                30,
                transactionRepository,
                repository,
                notifierService,
                paymentErrorNotifierService,
                fileStorageClient,
                auditUtilities,
                transactionSynchronizer,
                merchantConnector
        );
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        trx.setElaborationDateTime(LocalDateTime.now().minusDays(1)); // 1 giorno fa rispetto a oggi

        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        assertThrows( OperationNotAllowedException.class, () -> {
            service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, DOCUMENT_NUMBER);
        });
    }

    @Test
    void invoiceTransaction_shouldFetchPointOfSaleData_whenFranchiseNameOrPointOfSaleTypeIsNull() {
        PointOfSaleDTO pointOfSaleDTO = PointOfSaleDTO.builder()
                .franchiseName("Franchise Test")
                .type(PointOfSaleTypeEnum.PHYSICAL)
                .businessName("Business Name")
                .fiscalCode("FISCAL123")
                .vatNumber("VAT123")
                .build();
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        when(merchantConnector.getPointOfSale(MERCHANT_ID, POS_ID)).thenReturn(pointOfSaleDTO);
        when(notifierService.notify(any(TransactionInProgress.class), anyString())).thenReturn(true);

        service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, DOCUMENT_NUMBER);

        verify(merchantConnector, times(1)).getPointOfSale(MERCHANT_ID, POS_ID);
        assertEquals("Franchise Test", trx.getFranchiseName());
        assertEquals("PHYSICAL", trx.getPointOfSaleType());
        assertEquals(SyncTrxStatus.INVOICED, trx.getStatus());
    }

}
