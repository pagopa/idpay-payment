package it.gov.pagopa.payment.controller;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.service.payment.TransactionService;
import it.gov.pagopa.payment.utils.Utilities;
import it.gov.pagopa.payment.dto.UpdateTransactionsStatusRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class TransactionsControllerImpl implements TransactionsController {

    private final TransactionService transactionService;

    public TransactionsControllerImpl(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Override
    @PerformanceLog("FIND_ALL_TRANSACTIONS")
    public List<Transaction> findAll(
            String idTrxIssuer,
            String userId,
            LocalDateTime trxDateStart,
            LocalDateTime trxDateEnd,
            Long amountCents,
            Pageable pageable) {

        return transactionService.findAll(
                sanitize(idTrxIssuer),
                sanitize(userId),
                trxDateStart,
                trxDateEnd,
                amountCents,
                pageable
        );
    }

    @Override
    @PerformanceLog("FIND_TRANSACTIONS_BY_INITIATIVE_AND_USER")
    public List<Transaction> findByInitiativeIdAndUserId(String initiativeId, String userId) {
        return transactionService.findByInitiativeIdAndUserId(sanitize(initiativeId), sanitize(userId));
    }

    @Override
    @PerformanceLog("UPDATE_TRANSACTIONS_STATUS")
    public int updateTransactionsStatus(UpdateTransactionsStatusRequest request) {
        Set<String> sanitizedIds = request.transactionIds().stream()
                .map(this::sanitize)
                .collect(Collectors.toSet());
        return transactionService.updateTransactionsStatus(sanitizedIds, request.status());
    }

    private String sanitize(String value) {
        return Utilities.sanitizeString(value);
    }

}
