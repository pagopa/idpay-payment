package it.gov.pagopa.payment.service.payment.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;

import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.merchant.dto.PointOfSaleDTO;
import it.gov.pagopa.payment.connector.storage.FileStorageClient;
import it.gov.pagopa.payment.enums.PointOfSaleTypeEnum;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.*;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

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
        Mockito.when(file.getOriginalFilename()).thenReturn(FILENAME);
        Mockito.when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        Mockito.when(file.getContentType()).thenReturn("application/pdf");

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
                repository,
                notifierService,
                paymentErrorNotifierService,
                fileStorageClient,
                auditUtilities,
                merchantConnector
        );
    }

    @Test
    void invoiceTransaction_success() {
        Mockito.when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));

        PointOfSaleDTO pos = PointOfSaleDTO.builder()
            .franchiseName("Test")
            .type(PointOfSaleTypeEnum.PHYSICAL)
            .build();
        Mockito.when(merchantConnector.getPointOfSale(MERCHANT_ID, POS_ID))
            .thenReturn(pos);

        Mockito.when(notifierService.notify(any(), anyString())).thenReturn(true);
        service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, DOCUMENT_NUMBER);
        Mockito.verify(fileStorageClient).upload(any(), anyString(), anyString());
        Mockito.verify(repository).save(trx);
        Mockito.verify(auditUtilities).logInvoiceTransaction(any());
        assertEquals(SyncTrxStatus.INVOICED, trx.getStatus());
        assertEquals(FILENAME, trx.getInvoiceData().getFilename());
        assertEquals(DOCUMENT_NUMBER, trx.getInvoiceData().getDocNumber());
    }

    @Test
    void invoiceTransaction_transactionNotFound() {
        Mockito.when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());
        assertThrows(TransactionNotFoundOrExpiredException.class,
                () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, DOCUMENT_NUMBER));
        Mockito.verify(auditUtilities).logErrorInvoiceTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void invoiceTransaction_merchantMismatch() {
        trx.setMerchantId("otherMerchant");
        Mockito.when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        assertThrows(TransactionInvalidException.class,
                () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, DOCUMENT_NUMBER));
        Mockito.verify(auditUtilities).logErrorInvoiceTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void invoiceTransaction_posMismatch() {
        trx.setPointOfSaleId("otherPos");
        Mockito.when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        assertThrows(TransactionInvalidException.class,
                () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, DOCUMENT_NUMBER));
        Mockito.verify(auditUtilities).logErrorInvoiceTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void invoiceTransaction_statusNotCaptured() {
        trx.setStatus(SyncTrxStatus.CREATED);
        Mockito.when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        assertThrows(OperationNotAllowedException.class,
                () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, DOCUMENT_NUMBER));
        Mockito.verify(auditUtilities).logErrorInvoiceTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void invoiceTransaction_invalidFileFormat_shouldThrowInvalidInvoiceFormatException() {
        Mockito.when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        MultipartFile invalidFile = Mockito.mock(MultipartFile.class);
        Mockito.when(invalidFile.getOriginalFilename()).thenReturn("document.txt");
        InvalidInvoiceFormatException ex = assertThrows(InvalidInvoiceFormatException.class,
                () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, invalidFile, DOCUMENT_NUMBER));
        assertEquals("File must be a PDF or XML", ex.getMessage());
    }

    @Test
    void invoiceTransaction_nullFile_shouldThrowInvalidInvoiceFormatException() {
        Mockito.when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        InvalidInvoiceFormatException ex = assertThrows(InvalidInvoiceFormatException.class,
                () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, null, DOCUMENT_NUMBER));
        assertEquals("File is required", ex.getMessage());
    }

    @Test
    void invoiceTransaction_invalidFileExtension_shouldThrowInvalidInvoiceFormatException() {
        MultipartFile invalidFile = new MockMultipartFile("file", "invoice.txt", "text/plain", "dummy".getBytes());
        Mockito.when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));

        InvalidInvoiceFormatException ex = assertThrows(InvalidInvoiceFormatException.class,
            () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, invalidFile, DOCUMENT_NUMBER));
        assertEquals("File must be a PDF or XML", ex.getMessage());
    }


    @Test
    void invoiceTransaction_nullFileName_shouldThrowInvalidInvoiceFormatException() {
        Mockito.when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        MultipartFile fileWithNullName = Mockito.mock(MultipartFile.class);
        Mockito.when(fileWithNullName.getOriginalFilename()).thenReturn(null);
        InvalidInvoiceFormatException ex = assertThrows(InvalidInvoiceFormatException.class,
                () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, fileWithNullName, DOCUMENT_NUMBER));
        assertEquals("File must be a PDF or XML", ex.getMessage());
    }

    @Test
    void invoiceTransaction_runtimeException_shouldLogAndThrow() {
        Mockito.when(repository.findById(TRANSACTION_ID)).thenThrow(new RuntimeException("Generic error"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, DOCUMENT_NUMBER));
        assertEquals("Generic error", ex.getMessage());
        Mockito.verify(auditUtilities).logErrorInvoiceTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void invoiceTransaction_ioException_shouldLogAndThrow() {
        Mockito.when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        Mockito.doThrow(new RuntimeException(new IOException("IO error"))).when(fileStorageClient).upload(any(), anyString(), anyString());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, DOCUMENT_NUMBER));
        assertEquals("IO error", ex.getCause().getMessage());
        Mockito.verify(auditUtilities).logErrorInvoiceTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void sendInvoiceTransactionNotification_notifyReturnsFalse_shouldThrowInternalServerErrorException() {
        Mockito.when(notifierService.notify(any(), anyString())).thenReturn(false);
        assertThrows(TransactionNotFoundOrExpiredException.class,
                () -> service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, DOCUMENT_NUMBER));
    }

    @Test
    void invoiceTransaction_shouldSetCorrectInvoicePath() {
        Mockito.when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));

        PointOfSaleDTO pos = PointOfSaleDTO.builder()
            .franchiseName("Franchise Test")
            .type(PointOfSaleTypeEnum.PHYSICAL)
            .build();

        Mockito.when(merchantConnector.getPointOfSale(MERCHANT_ID, POS_ID)).thenReturn(pos);
        Mockito.when(notifierService.notify(any(), anyString())).thenReturn(true);
        service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, DOCUMENT_NUMBER);
        String expectedPath = String.format("invoices/merchant/%s/pos/%s/transaction/%s/invoice/%s",
                MERCHANT_ID, POS_ID, trx.getId(), FILENAME);
        Mockito.verify(fileStorageClient).upload(any(), eq(expectedPath), anyString());
    }

    @Test
    void shouldThrowOperationNotAllowedException_whenTrxIsTooRecent() {
        service = new CommonInvoiceServiceImpl(
                30,
                repository,
                notifierService,
                paymentErrorNotifierService,
                fileStorageClient,
                auditUtilities,
                merchantConnector
        );

        trx.setElaborationDateTime(LocalDateTime.now().minusDays(1)); // 1 giorno fa rispetto a oggi

        Mockito.when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
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

        Mockito.when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        Mockito.when(merchantConnector.getPointOfSale(MERCHANT_ID, POS_ID)).thenReturn(pointOfSaleDTO);
        Mockito.when(notifierService.notify(any(), anyString())).thenReturn(true);

        service.invoiceTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, DOCUMENT_NUMBER);

        Mockito.verify(merchantConnector, Mockito.times(1)).getPointOfSale(MERCHANT_ID, POS_ID);
        assertEquals("Franchise Test", trx.getFranchiseName());
        assertEquals("PHYSICAL", trx.getPointOfSaleType());
        assertEquals(SyncTrxStatus.INVOICED, trx.getStatus());
    }

}
