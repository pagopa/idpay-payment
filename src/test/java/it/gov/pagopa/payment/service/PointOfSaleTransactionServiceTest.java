package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.connector.storage.FileStorageClient;
import it.gov.pagopa.payment.dto.DownloadInvoiceResponseDTO;
import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.exception.custom.TransactionMissingParametersException;
import it.gov.pagopa.payment.model.InvoiceData;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointOfSaleTransactionServiceTest {

    @Mock
    private TransactionService transactionService;
    @Mock
    private FileStorageClient fileStorageClient;

    @InjectMocks
    private PointOfSaleTransactionServiceImpl pointOfSaleTransactionService;

    @Test
    void getPointOfSaleTransactionList_shouldDelegateToTransactionService() {
        Transaction transaction1 = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        Transaction transaction2 = TransactionFaker.mockInstance(2, SyncTrxStatus.AUTHORIZED);

        Page<Transaction> expectedPage = new PageImpl<>(List.of(transaction1, transaction2));
        TrxFiltersDTO filters = new TrxFiltersDTO();
        Pageable pageable = Pageable.unpaged();

        when(transactionService.getTransactionsByFilters(any(TrxFiltersDTO.class), any(Pageable.class)))
                .thenReturn(expectedPage);

        Page<Transaction> resultPage = pointOfSaleTransactionService.getPointOfSaleTransactions(filters, pageable);

        assertNotNull(resultPage);
        assertEquals(2, resultPage.getTotalElements());
        assertEquals(transaction1.getId(), resultPage.getContent().get(0).getId());
        assertEquals(transaction2.getId(), resultPage.getContent().get(1).getId());
        verify(transactionService).getTransactionsByFilters(filters, pageable);
    }

    @Test
    void downloadTransactionInvoice_shouldReturnSignedUrlForInvoice() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.INVOICED);
        transaction.setInvoiceData(new InvoiceData("invoice.pdf", "DOC001"));
        when(transactionService.getTransactionByIdAndMerchantId("TRX1", "MERCHANT1"))
                .thenReturn(transaction);
        when(fileStorageClient.getInvoiceFileSignedUrl("invoices/merchant/MERCHANT1/pos/POS1/transaction/TRX1/invoice/invoice.pdf"))
                .thenReturn("https://signed-url/invoice");

        DownloadInvoiceResponseDTO response = pointOfSaleTransactionService.downloadTransactionInvoice("MERCHANT1", "POS1", "TRX1");

        assertNotNull(response);
        assertEquals("https://signed-url/invoice", response.getInvoiceUrl());
    }

    @Test
    void downloadTransactionInvoice_shouldReturnSignedUrlForCreditNote() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.REFUNDED);
        transaction.setCreditNoteData(new InvoiceData("credit-note.pdf", "DOC002"));
        when(transactionService.getTransactionByIdAndMerchantId("TRX1", "MERCHANT1"))
                .thenReturn(transaction);
        when(fileStorageClient.getInvoiceFileSignedUrl("invoices/merchant/MERCHANT1/pos/POS1/transaction/TRX1/creditNote/credit-note.pdf"))
                .thenReturn("https://signed-url/credit-note");

        DownloadInvoiceResponseDTO response = pointOfSaleTransactionService.downloadTransactionInvoice("MERCHANT1", "POS1", "TRX1");

        assertNotNull(response);
        assertEquals("https://signed-url/credit-note", response.getInvoiceUrl());
    }

    @ParameterizedTest
    @CsvSource({
            ", POS1, TRX1",
            "MERCHANT1, , TRX1",
            "MERCHANT1, POS1, ",
            "' ', POS1, TRX1"
    })
    void downloadTransactionInvoice_shouldThrowMissingParametersExceptionWhenInputsAreInvalid(
            String merchantId, String pointOfSaleId, String transactionId) {

        assertThrows(
                TransactionMissingParametersException.class,
                () -> pointOfSaleTransactionService.downloadTransactionInvoice(merchantId, pointOfSaleId, transactionId)
        );
        verifyNoInteractions(transactionService, fileStorageClient);
    }

    @Test
    void downloadTransactionInvoice_shouldThrowWhenTransactionStatusIsNull() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        when(transactionService.getTransactionByIdAndMerchantId("TRX1", "MERCHANT1"))
                .thenReturn(transaction);

        assertThrows(
                TransactionInvalidException.class,
                () -> pointOfSaleTransactionService.downloadTransactionInvoice("MERCHANT1", "POS1", "TRX1")
        );
        verifyNoInteractions(fileStorageClient);
    }

    @Test
    void downloadTransactionInvoice_shouldThrowWhenTransactionStatusIsUnsupported() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        when(transactionService.getTransactionByIdAndMerchantId("TRX1", "MERCHANT1"))
                .thenReturn(transaction);

        assertThrows(
                TransactionInvalidException.class,
                () -> pointOfSaleTransactionService.downloadTransactionInvoice("MERCHANT1", "POS1", "TRX1")
        );
        verifyNoInteractions(fileStorageClient);
    }

    @Test
    void downloadTransactionInvoice_shouldThrowWhenInvoiceDataIsMissing() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.REWARDED);
        transaction.setInvoiceData(null);
        when(transactionService.getTransactionByIdAndMerchantId("TRX1", "MERCHANT1"))
                .thenReturn(transaction);

        assertThrows(
                TransactionInvalidException.class,
                () -> pointOfSaleTransactionService.downloadTransactionInvoice("MERCHANT1", "POS1", "TRX1")
        );
        verifyNoInteractions(fileStorageClient);
    }

    @Test
    void downloadTransactionInvoice_shouldThrowWhenInvoiceFilenameIsEmpty() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.INVOICED);
        transaction.setInvoiceData(new InvoiceData("", "DOC001"));
        when(transactionService.getTransactionByIdAndMerchantId("TRX1", "MERCHANT1"))
                .thenReturn(transaction);

        assertThrows(
                TransactionInvalidException.class,
                () -> pointOfSaleTransactionService.downloadTransactionInvoice("MERCHANT1", "POS1", "TRX1")
        );
        verifyNoInteractions(fileStorageClient);
    }
}