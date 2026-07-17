package it.gov.pagopa.payment.controller;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.MerchantTransactionsListDTO;
import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.service.MerchantTransactionService;
import it.gov.pagopa.payment.utils.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class MerchantTransactionControllerImpl implements MerchantTransactionController {

    private static final String LOG_GET_MERCHANT_TRANSACTIONS = "[GET_MERCHANT_TRANSACTIONS]";
    private static final String LOG_GET_MERCHANT_TRANSACTIONS_PROCESSED = "[GET_MERCHANT_TRANSACTIONS_PROCESSED]";

    private final MerchantTransactionService merchantTransactionService;

    public MerchantTransactionControllerImpl(MerchantTransactionService merchantTransactionService) {
        this.merchantTransactionService = merchantTransactionService;
    }

    @Override
    @PerformanceLog("GET_MERCHANT_TRANSACTIONS")
    public MerchantTransactionsListDTO getMerchantTransactions(
            String merchantId,
            String initiativeId,
            String fiscalCode,
            String status,
            Pageable pageable) {

        String sanitizedMerchantId = sanitize(merchantId);
        logRequest(LOG_GET_MERCHANT_TRANSACTIONS, sanitizedMerchantId);

        return merchantTransactionService.getMerchantTransactions(
                sanitizedMerchantId,
                sanitize(initiativeId),
                sanitize(fiscalCode),
                sanitize(status),
                pageable
        );
    }

    @Override
    @PerformanceLog("GET_MERCHANT_TRANSACTIONS_PROCESSED")
    public MerchantTransactionsListDTO getMerchantTransactionsProcessed(
            String merchantId,
            String organizationRole,
            String initiativeId,
            String fiscalCode,
            String status,
            String rewardBatchId,
            String rewardBatchTrxStatus,
            String pointOfSaleId,
            String trxCode,
            Pageable pageable) {

        String sanitizedMerchantId = sanitize(merchantId);
        logRequest(LOG_GET_MERCHANT_TRANSACTIONS_PROCESSED, sanitizedMerchantId);

        String sanitizedStatus = sanitize(status);
        List<String> processedStatuses;

        // Se lo status è valido, filtra solo per quello, altrimenti applica la lista di default
        if (StringUtils.hasText(sanitizedStatus) &&
                TrxFiltersDTO.PROCESSED_ALLOWED_STATUSES.contains(sanitizedStatus.toUpperCase())) {
            processedStatuses = List.of(sanitizedStatus.toUpperCase());
        } else {
            processedStatuses = TrxFiltersDTO.PROCESSED_ALLOWED_STATUSES;
        }

        return merchantTransactionService.getMerchantTransactionsProcessed(
                sanitizedMerchantId,
                sanitize(organizationRole),
                sanitize(initiativeId),
                sanitize(fiscalCode),
                processedStatuses,
                sanitize(rewardBatchId),
                sanitize(rewardBatchTrxStatus),
                sanitize(pointOfSaleId),
                sanitize(trxCode),
                pageable
        );
    }

    @Override
    public List<String> getProcessedTransactionStatuses(String organizationRole) {
        return merchantTransactionService.getProcessedTransactionStatuses(sanitize(organizationRole));
    }

    private void logRequest(String logPrefix, String merchantId) {
        log.info("{} Merchant {} requested to retrieve transactions", logPrefix, Utilities.sanitizeForLog(merchantId));
    }

    private String sanitize(String value) {
        return Utilities.sanitizeString(value);
    }
}