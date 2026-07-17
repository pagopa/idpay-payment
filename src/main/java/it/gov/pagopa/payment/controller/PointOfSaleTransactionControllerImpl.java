package it.gov.pagopa.payment.controller;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.DownloadInvoiceResponseDTO;
import it.gov.pagopa.payment.dto.PointOfSaleTransactionDTO;
import it.gov.pagopa.payment.dto.PointOfSaleTransactionsListDTO;
import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.dto.mapper.PointOfSaleTransactionMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.exception.custom.PointOfSaleNotAllowedException;
import it.gov.pagopa.payment.service.PointOfSaleTransactionService;
import it.gov.pagopa.payment.utils.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class PointOfSaleTransactionControllerImpl implements PointOfSaleTransactionController {

    private static final String LOG_GET_POINT_OF_SALE_TRANSACTIONS = "[GET_POINT-OF-SALE_TRANSACTIONS]";
    private static final String LOG_DOWNLOAD_TRANSACTION = "[DOWNLOAD_TRANSACTION]";
    private static final String POINT_OF_SALE_MISMATCH_MESSAGE = "Point of sale mismatch: expected [%s], but received [%s]";

    private final PointOfSaleTransactionService pointOfSaleTransactionService;
    private final PointOfSaleTransactionMapper mapper;

    public PointOfSaleTransactionControllerImpl(
            PointOfSaleTransactionService pointOfSaleTransactionService,
            PointOfSaleTransactionMapper mapper
    ) {
        this.pointOfSaleTransactionService = pointOfSaleTransactionService;
        this.mapper = mapper;
    }

    @Override
    @PerformanceLog("GET_POS_TRANSACTIONS")
    public PointOfSaleTransactionsListDTO getPointOfSaleTransactions(
            String merchantId,
            String tokenPointOfSaleId,
            String initiativeId,
            String pointOfSaleId,
            String fiscalCode,
            String status,
            String productGtin,
            String trxCode,
            Pageable pageable) {

        return retrievePointOfSaleTransactions(
                merchantId,
                tokenPointOfSaleId,
                initiativeId,
                pointOfSaleId,
                productGtin,
                fiscalCode,
                status,
                trxCode,
                pageable
        );
    }

    @Override
    @PerformanceLog("GET_POS_TRANSACTIONS_PROCESSED")
    public PointOfSaleTransactionsListDTO getPointOfSaleTransactionsProcessed(
            String merchantId,
            String tokenPointOfSaleId,
            String initiativeId,
            String pointOfSaleId,
            String productGtin,
            String fiscalCode,
            String status,
            String trxCode,
            Pageable pageable) {

        return retrievePointOfSaleTransactions(
                merchantId,
                tokenPointOfSaleId,
                initiativeId,
                pointOfSaleId,
                productGtin,
                fiscalCode,
                status,
                trxCode,
                pageable
        );
    }

    @Override
    @PerformanceLog("DOWNLOAD_POS_INVOICE")
    public DownloadInvoiceResponseDTO downloadInvoiceFile(
            String merchantId,
            String tokenPointOfSaleId,
            String pointOfSaleId,
            String transactionId) {

        String sanitizedMerchantId = sanitize(merchantId);
        String sanitizedTokenPointOfSaleId = sanitize(tokenPointOfSaleId);
        String sanitizedPointOfSaleId = sanitize(pointOfSaleId);
        String sanitizedTransactionId = sanitize(transactionId);

        log.info("{} Requested to download invoice for transaction {}",
                LOG_DOWNLOAD_TRANSACTION,
                Utilities.sanitizeForLog(sanitizedTransactionId));

        validatePointOfSaleAccess(sanitizedTokenPointOfSaleId, sanitizedPointOfSaleId);

        return pointOfSaleTransactionService.downloadTransactionInvoice(
                sanitizedMerchantId,
                sanitizedPointOfSaleId,
                sanitizedTransactionId
        );
    }

    private PointOfSaleTransactionsListDTO retrievePointOfSaleTransactions(
            String merchantId,
            String tokenPointOfSaleId,
            String initiativeId,
            String pointOfSaleId,
            String productGtin,
            String fiscalCode,
            String status,
            String trxCode,
            Pageable pageable) {

        TrxFiltersDTO filters = buildSanitizedFilters(
                merchantId,
                initiativeId,
                pointOfSaleId,
                productGtin,
                fiscalCode,
                status,
                trxCode
        );
        String sanitizedTokenPointOfSaleId = sanitize(tokenPointOfSaleId);

        log.info("{} Point Of Sale {} requested to retrieve transactions",
                LOG_GET_POINT_OF_SALE_TRANSACTIONS,
                Utilities.sanitizeForLog(filters.getPointOfSaleId()));

        validatePointOfSaleAccess(sanitizedTokenPointOfSaleId, filters.getPointOfSaleId());

        Page<Transaction> transactions = pointOfSaleTransactionService.getPointOfSaleTransactions(filters, pageable);
        return toPointOfSaleTransactionsListDTO(transactions, filters.getFiscalCode());
    }

    private TrxFiltersDTO buildSanitizedFilters(
            String merchantId,
            String initiativeId,
            String pointOfSaleId,
            String productGtin,
            String fiscalCode,
            String status,
            String trxCode) {

        TrxFiltersDTO filters = new TrxFiltersDTO();
        filters.setMerchantId(sanitize(merchantId));
        filters.setInitiativeId(sanitize(initiativeId));
        filters.setPointOfSaleId(sanitize(pointOfSaleId));
        filters.setProductGtin(sanitize(productGtin));
        filters.setFiscalCode(sanitize(fiscalCode));
        filters.setStatus(sanitize(status));
        filters.setTrxCode(sanitize(trxCode));
        return filters;
    }

    private PointOfSaleTransactionsListDTO toPointOfSaleTransactionsListDTO(
            Page<Transaction> transactions,
            String sanitizedFiscalCode) {

        List<PointOfSaleTransactionDTO> dtos = transactions.getContent().stream()
                .map(tx -> mapper.toPointOfSaleTransactionDTO(tx, sanitizedFiscalCode))
                .toList();

        return new PointOfSaleTransactionsListDTO(
                dtos,
                transactions.getNumber(),
                transactions.getSize(),
                (int) transactions.getTotalElements(),
                transactions.getTotalPages()
        );
    }

    private String sanitize(String value) {
        return Utilities.sanitizeString(value);
    }

    private void validatePointOfSaleAccess(String tokenPointOfSaleId, String pointOfSaleId) {
        if (tokenPointOfSaleId != null && !tokenPointOfSaleId.equals(pointOfSaleId)) {
            throw new PointOfSaleNotAllowedException(
                    POINT_OF_SALE_MISMATCH_MESSAGE.formatted(tokenPointOfSaleId, pointOfSaleId));
        }
    }
}