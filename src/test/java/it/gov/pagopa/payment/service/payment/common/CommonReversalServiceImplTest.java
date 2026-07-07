package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.utils.TransactionSynchronizer;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.storage.FileStorageClient;
import it.gov.pagopa.payment.entity.Transaction;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommonReversalServiceImplTest {
    @Mock
    private TransactionRepository transactionRepository;
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
    @Mock private TransactionSynchronizer transactionSynchronizer;

    @InjectMocks
    private CommonReversalServiceImpl service;

    private static final String TRANSACTION_ID = "trxId";
    private static final String MERCHANT_ID = "merchantId";
    private static final String POS_ID = "posId";
    private static final String FILENAME = "creditNote.pdf";
    public static final String CREDIT_NOTE_NUMBER = "FPR 192/25";

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
    }

    @Test
    void reversalTransaction_success() {
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        when(notifierService.notify(any(TransactionInProgress.class), anyString())).thenReturn(true);
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));

        service.reversalTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, CREDIT_NOTE_NUMBER);

        verify(fileStorageClient).upload(any(), anyString(), anyString());
        verify(repository).deleteById(TRANSACTION_ID);
        verify(auditUtilities).logReverseTransaction(any());
        assertEquals(SyncTrxStatus.REFUNDED, trx.getStatus());
        assertEquals(FILENAME, trx.getCreditNoteData().getFilename());
    }

    @Test
    void reversalTransaction_transactionNotFound() {
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());
        assertThrows(TransactionNotFoundOrExpiredException.class,
                () -> service.reversalTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, CREDIT_NOTE_NUMBER));
        verify(auditUtilities).logErrorReversalTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void reversalTransaction_merchantMismatch() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        trx.setMerchantId("otherMerchant");
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        assertThrows(TransactionInvalidException.class,
                () -> service.reversalTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, CREDIT_NOTE_NUMBER));
        verify(auditUtilities).logErrorReversalTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void reversalTransaction_posMismatch() {
        trx.setPointOfSaleId("otherPos");
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        assertThrows(TransactionInvalidException.class,
                () -> service.reversalTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, CREDIT_NOTE_NUMBER));
        verify(auditUtilities).logErrorReversalTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void reversalTransaction_statusNotCaptured() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        trx.setStatus(SyncTrxStatus.CREATED);
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        assertThrows(OperationNotAllowedException.class,
                () -> service.reversalTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, CREDIT_NOTE_NUMBER));
        verify(auditUtilities).logErrorReversalTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void reversalTransaction_runtimeException_shouldLogAndThrow() {
        when(repository.findById(TRANSACTION_ID)).thenThrow(new RuntimeException("Generic error"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.reversalTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, CREDIT_NOTE_NUMBER));
        assertEquals("Generic error", ex.getMessage());
        verify(auditUtilities).logErrorReversalTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void reversalTransaction_ioException_shouldLogAndThrow() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        doThrow(new RuntimeException(new IOException("IO error"))).when(fileStorageClient).upload(any(), anyString(), anyString());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.reversalTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, CREDIT_NOTE_NUMBER));
        assertEquals("IO error", ex.getCause().getMessage());
        verify(auditUtilities).logErrorReversalTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void sendReversedTransactionNotification_notifyReturnsFalse_shouldThrowTransactionNotFoundOrExpiredException() {
        when(notifierService.notify(any(TransactionInProgress.class), anyString())).thenReturn(false);
        assertThrows(TransactionNotFoundOrExpiredException.class,
                () -> service.reversalTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, CREDIT_NOTE_NUMBER));
    }

    @Test
    void reversalTransaction_shouldSetCorrectInvoicePath() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        when(notifierService.notify(any(TransactionInProgress.class), anyString())).thenReturn(true);
        service.reversalTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file, CREDIT_NOTE_NUMBER);
        String expectedPath = String.format("invoices/merchant/%s/pos/%s/transaction/%s/creditNote/%s",
                MERCHANT_ID, POS_ID, trx.getId(), FILENAME);
        verify(fileStorageClient).upload(any(), eq(expectedPath), anyString());
    }

    @Test
    void reversalTransaction_invalidFileFormat_shouldThrowInvalidInvoiceFormatException() {
        when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        MultipartFile invalidFile = mock(MultipartFile.class);
        when(invalidFile.getOriginalFilename()).thenReturn("document.txt");
        InvalidInvoiceFormatException ex = assertThrows(InvalidInvoiceFormatException.class,
                () -> service.reversalTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, invalidFile, CREDIT_NOTE_NUMBER));
        assertEquals("File must be a PDF or XML", ex.getMessage());
    }

}
