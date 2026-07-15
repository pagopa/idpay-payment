package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.connector.storage.FileStorageClient;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.DownloadInvoiceResponseDTO;
import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.model.InvoiceData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Objects;


@Service
public class PointOfSaleTransactionServiceImpl implements PointOfSaleTransactionService {

    private static final String INVOICE_FOLDER = "invoice";
    private static final String CREDIT_NOTE_FOLDER = "creditNote";
    private static final String TRANSACTION_MISSING_INVOICE_MESSAGE = "Invoice missing from transaction for which download was required";

    private final TransactionService transactionService;
    private final FileStorageClient fileStorageClient;

    public PointOfSaleTransactionServiceImpl(
            TransactionService transactionService,
            FileStorageClient fileStorageClient) {
        this.transactionService = transactionService;
        this.fileStorageClient = fileStorageClient;
    }

    @Override
    public Page<Transaction> getPointOfSaleTransactions(TrxFiltersDTO filters,
                                                        Pageable pageable) {
        Objects.requireNonNull(filters, "filters must not be null");
        Objects.requireNonNull(pageable, "pageable must not be null");
        return transactionService.getTransactionsByFilters(filters, pageable);
    }

    @Override
    public DownloadInvoiceResponseDTO downloadTransactionInvoice(
            String merchantId,
            String pointOfSaleId,
            String transactionId
    ) {
        Objects.requireNonNull(merchantId, "merchantId must not be null");
        Objects.requireNonNull(pointOfSaleId, "pointOfSaleId must not be null");
        Objects.requireNonNull(transactionId, "transactionId must not be null");

        Transaction transaction = transactionService.getTransactionByIdAndMerchantId(transactionId, merchantId);
        InvoiceDocument invoiceDocument = resolveInvoiceDocument(transaction);
        String blobPath = buildBlobPath(
                merchantId,
                pointOfSaleId,
                transactionId,
                invoiceDocument.folderName(),
                invoiceDocument.invoiceData().getFilename()
        );

        return DownloadInvoiceResponseDTO.builder()
                .invoiceUrl(fileStorageClient.getInvoiceFileSignedUrl(blobPath))
                .build();
    }

    private InvoiceDocument resolveInvoiceDocument(Transaction transaction) {
        SyncTrxStatus status = transaction.getStatus();
        if (status == null) {
            throw buildMissingInvoiceException();
        }

        InvoiceDocument invoiceDocument = switch (status) {
            case INVOICED, REWARDED -> new InvoiceDocument(transaction.getInvoiceData(), INVOICE_FOLDER);
            case REFUNDED -> new InvoiceDocument(transaction.getCreditNoteData(), CREDIT_NOTE_FOLDER);
            default -> throw buildMissingInvoiceException();
        };

        validateInvoiceData(invoiceDocument.invoiceData());
        return invoiceDocument;
    }

    private void validateInvoiceData(InvoiceData invoiceData) {
        if (invoiceData == null || invoiceData.getFilename() == null) {
            throw buildMissingInvoiceException();
        }
    }

    private String buildBlobPath(
            String merchantId,
            String pointOfSaleId,
            String transactionId,
            String folderName,
            String filename
    ) {
        return String.format(
                "invoices/merchant/%s/pos/%s/transaction/%s/%s/%s",
                merchantId,
                pointOfSaleId,
                transactionId,
                folderName,
                filename
        );
    }

    private TransactionInvalidException buildMissingInvoiceException() {
        return new TransactionInvalidException(
                PaymentConstants.ExceptionCode.GENERIC_ERROR,
                TRANSACTION_MISSING_INVOICE_MESSAGE
        );
    }

    private record InvoiceDocument(InvoiceData invoiceData, String folderName) {
    }
}
