package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.ExpiredTransactionsProcessedDTO;
import it.gov.pagopa.payment.service.payment.TransactionInProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ExpiredTransactionsControllerImpl implements ExpiredTransactionsController {

    private final TransactionInProgressService transactionInProgressService;

    public ExpiredTransactionsControllerImpl(TransactionInProgressService transactionInProgressService) {
        this.transactionInProgressService = transactionInProgressService;
    }

    @Override
    public ExpiredTransactionsProcessedDTO findAndUpdateStatus(String initiativeId) {
        return ExpiredTransactionsProcessedDTO.builder()
                .processedTransactions(
                    transactionInProgressService.findAndUpdateExpiredTransactionsStatus(initiativeId)
                )
                .build();
    }

    @Override
    public ExpiredTransactionsProcessedDTO findAndSendStaleExpired(String initiativeId) {
        return ExpiredTransactionsProcessedDTO.builder()
                .processedTransactions(
                        transactionInProgressService.sendEventForStaleExpiredTransactions(initiativeId)
                )
                .build();
    }

}
