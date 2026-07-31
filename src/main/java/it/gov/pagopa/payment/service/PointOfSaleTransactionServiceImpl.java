package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.connector.storage.FileStorageClient;
import it.gov.pagopa.payment.dto.DownloadInvoiceResponseDTO;
import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.exception.custom.TransactionMissingParametersException;
import it.gov.pagopa.payment.model.InvoiceData;
import it.gov.pagopa.payment.service.payment.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode.TRANSACTIONS_MISSING_MANDATORY_FILTERS;
import static it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode.TRANSACTION_INVALID_REQUEST;
import static it.gov.pagopa.payment.constants.PaymentConstants.ExceptionMessage.TRANSACTION_MISSING_INVOICE_MESSAGE;
import static it.gov.pagopa.payment.constants.PaymentConstants.buildMissingFiltersMessage;

@Service
@Slf4j
public class PointOfSaleTransactionServiceImpl implements PointOfSaleTransactionService {

    private static final String INVOICE_FOLDER = "invoice";
    private static final String CREDIT_NOTE_FOLDER = "creditNote";

    private final TransactionService transactionService;
    private final FileStorageClient fileStorageClient;

    public PointOfSaleTransactionServiceImpl(
            TransactionService transactionService,
            FileStorageClient fileStorageClient) {
        this.transactionService = transactionService;
        this.fileStorageClient = fileStorageClient;
    }

    @Override
    public Page<Transaction> getPointOfSaleTransactions(TrxFiltersDTO filters, Pageable pageable) {
        return transactionService.getTransactionsByFilters(filters, pageable);
    }

    @Override
    public DownloadInvoiceResponseDTO downloadTransactionInvoice(
            String merchantId,
            String pointOfSaleId,
            String transactionId) {

        if (!StringUtils.hasText(merchantId) ||
                !StringUtils.hasText(pointOfSaleId) ||
                !StringUtils.hasText(transactionId)) {
            throw new TransactionMissingParametersException(
                    TRANSACTIONS_MISSING_MANDATORY_FILTERS,
                    buildMissingFiltersMessage("merchantId", "pointOfSaleId", "transactionId")
            );
        }

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
        if (invoiceData == null || !org.springframework.util.StringUtils.hasText(invoiceData.getFilename())) {
            throw buildMissingInvoiceException();
        }
    }

    private String buildBlobPath(
            String merchantId,
            String pointOfSaleId,
            String transactionId,
            String folderName,
            String filename) {
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
                TRANSACTION_INVALID_REQUEST,
                TRANSACTION_MISSING_INVOICE_MESSAGE
        );
    }

    private record InvoiceDocument(InvoiceData invoiceData, String folderName) {
    }
}