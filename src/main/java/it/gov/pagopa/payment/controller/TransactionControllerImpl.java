package it.gov.pagopa.payment.controller;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.TransactionDTO;
import it.gov.pagopa.payment.service.TransactionService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionControllerImpl implements TransactionController {
    private final TransactionService transactionService;

    public TransactionControllerImpl(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Override
    @PerformanceLog("GET_TRANSACTION")
    public TransactionDTO getTransaction(String transactionId, String userId) {
        return transactionService.getTransaction(transactionId, userId);
    }
}
