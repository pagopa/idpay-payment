package it.gov.pagopa.payment.controller;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class TransactionControllerImpl implements TransactionController {
    private final TransactionService transactionService;
    public TransactionControllerImpl(TransactionService transactionService) {
        this.transactionService = transactionService;
    }
    @Override
    @PerformanceLog("GET_TRANSACTION")
    public TransactionInProgress getTransaction(String transactionId, String userId) {
        log.info("[GET_TRANSACTION] User {} requested to retrieve transaction {}", userId, transactionId);
        return transactionService.getTransaction(transactionId, userId);
    }

    @Override
    @PerformanceLog("GET_STATUS_TRANSACTION")
    public SyncTrxStatus getStatusTransaction(String transactionId, String merchantId) {
        log.info("[GET_STATUS_TRANSACTION] Merchant{} requested to retrieve status of transaction{}", merchantId,transactionId);
        return transactionService.getStatusTransaction(transactionId, merchantId);
    }
}
