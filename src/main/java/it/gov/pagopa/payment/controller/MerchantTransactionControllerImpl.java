package it.gov.pagopa.payment.controller;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.MerchantTransactionsListDTO;
import it.gov.pagopa.payment.service.MerchantTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class MerchantTransactionControllerImpl implements MerchantTransactionController {
    private final MerchantTransactionService merchantTransactionService;
    public MerchantTransactionControllerImpl(MerchantTransactionService merchantTransactionService) {
        this.merchantTransactionService = merchantTransactionService;
    }

    @Override
    @PerformanceLog("GET_MERCHANT_TRANSACTIONS")
    public MerchantTransactionsListDTO getMerchantTransactions(String merchantId, String initiativeId, String fiscalCode, String status, Pageable pageable) {
        log.info("[GET_MERCHANT_TRANSACTIONS] Merchant {} requested to retrieve transactions", merchantId);
        return merchantTransactionService.getMerchantTransactions(merchantId, initiativeId, fiscalCode, status, pageable);
    }
}
