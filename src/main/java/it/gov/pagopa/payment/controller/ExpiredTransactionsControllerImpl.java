package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.ExpiredTransactionsProcessedDTO;
import it.gov.pagopa.payment.service.payment.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ExpiredTransactionsControllerImpl implements ExpiredTransactionsController {

    private final TransactionService transactionService;

    public ExpiredTransactionsControllerImpl(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Override
    public ExpiredTransactionsProcessedDTO findAndUpdateStatus(String initiativeId) {
        return ExpiredTransactionsProcessedDTO.builder()
                .processedTransactions(
                        transactionService.findAndUpdateExpiredTransactionsStatus(initiativeId)
                )
                .build();
    }

    @Override
    public ExpiredTransactionsProcessedDTO findAndSendStaleExpired(String initiativeId) {
        return ExpiredTransactionsProcessedDTO.builder()
                .processedTransactions(
                        transactionService.sendEventForStaleExpiredTransactions(initiativeId)
                )
                .build();
    }

}
