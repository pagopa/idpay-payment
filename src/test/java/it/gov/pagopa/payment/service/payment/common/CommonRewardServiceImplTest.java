package it.gov.pagopa.payment.service.payment.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.storage.FileStorageClient;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

class CommonRewardServiceImplTest {
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

    @InjectMocks
    private CommonRewardServiceImpl service;

    private final String TRANSACTION_ID = "trxId";
    private final String MERCHANT_ID = "merchantId";
    private final String POS_ID = "posId";
    private final String FILENAME = "invoice.pdf";

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
    }

    @Test
    void rewardTransaction_success() {
        Mockito.when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        Mockito.when(notifierService.notify(any(), anyString())).thenReturn(true);

        service.rewardTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file);

        Mockito.verify(fileStorageClient).upload(any(), anyString(), anyString());
        Mockito.verify(repository).deleteById(TRANSACTION_ID);
        Mockito.verify(auditUtilities).logRewardTransaction(any());
        assertEquals(SyncTrxStatus.REWARDED, trx.getStatus());
        assertEquals(FILENAME, trx.getInvoiceFile().getFilename());
    }

    @Test
    void rewardTransaction_transactionNotFound() {
        Mockito.when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());
        assertThrows(TransactionNotFoundOrExpiredException.class,
                () -> service.rewardTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file));
        Mockito.verify(auditUtilities).logErrorRewardTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void rewardTransaction_merchantMismatch() {
        trx.setMerchantId("otherMerchant");
        Mockito.when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        assertThrows(TransactionInvalidException.class,
                () -> service.rewardTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file));
        Mockito.verify(auditUtilities).logErrorRewardTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void rewardTransaction_posMismatch() {
        trx.setPointOfSaleId("otherPos");
        Mockito.when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        assertThrows(TransactionInvalidException.class,
                () -> service.rewardTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file));
        Mockito.verify(auditUtilities).logErrorRewardTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void rewardTransaction_statusNotCaptured() {
        trx.setStatus(SyncTrxStatus.CREATED);
        Mockito.when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        assertThrows(OperationNotAllowedException.class,
                () -> service.rewardTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file));
        Mockito.verify(auditUtilities).logErrorRewardTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void rewardTransaction_runtimeException_shouldLogAndThrow() {
        Mockito.when(repository.findById(TRANSACTION_ID)).thenThrow(new RuntimeException("Generic error"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.rewardTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file));
        assertEquals("Generic error", ex.getMessage());
        Mockito.verify(auditUtilities).logErrorRewardTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void rewardTransaction_ioException_shouldLogAndThrow() {
        Mockito.when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        Mockito.doThrow(new RuntimeException(new IOException("IO error"))).when(fileStorageClient).upload(any(), anyString(), anyString());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.rewardTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file));
        assertEquals("IO error", ex.getCause().getMessage());
        Mockito.verify(auditUtilities).logErrorRewardTransaction(TRANSACTION_ID, MERCHANT_ID);
    }

    @Test
    void sendRewardedTransactionNotification_notifyReturnsFalse_shouldThrowTransactionNotFoundOrExpiredException() {
        Mockito.when(notifierService.notify(any(), anyString())).thenReturn(false);
        assertThrows(TransactionNotFoundOrExpiredException.class,
                () -> service.rewardTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file));
    }

    @Test
    void sendRewardedTransactionNotification_genericException_shouldLogAndThrow() {
        Mockito.when(notifierService.notify(any(), anyString())).thenReturn(false);
        assertThrows(Exception.class,
            () -> service.rewardTransaction(TRANSACTION_ID + "_ERROR", MERCHANT_ID, POS_ID, file));
        // TODO check log
    }

    @Test
    void rewardTransaction_shouldSetCorrectInvoicePath() {
        Mockito.when(repository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));
        Mockito.when(notifierService.notify(any(), anyString())).thenReturn(true);
        service.rewardTransaction(TRANSACTION_ID, MERCHANT_ID, POS_ID, file);
        String expectedPath = String.format("invoices/merchant/%s/pos/%s/transaction/%s/%s",
                MERCHANT_ID, POS_ID, trx.getId(), FILENAME);
        Mockito.verify(fileStorageClient).upload(any(), eq(expectedPath), anyString());
    }
}
