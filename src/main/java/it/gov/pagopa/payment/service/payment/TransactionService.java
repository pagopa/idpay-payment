package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionService {

    void generateTrxCodeAndSave(Transaction trx, String flowName);

    List<Transaction> findAll(
            String idTrxIssuer,
            String userId,
            LocalDateTime trxDateStart,
            LocalDateTime trxDateEnd,
            Long amountCents,
            Pageable pageable);

    List<Transaction> findByInitiativeIdAndUserId(String initiativeId, String userId);

    Page<Transaction> getTransactionsByFilters(TrxFiltersDTO filters, Pageable pageable);

    Transaction getTransactionByIdAndMerchantId(String transactionId, String merchantId);

    Page<Transaction> getMerchantTransactionByFilter(TrxFiltersDTO filters, Pageable pageable);

    long findAndUpdateExpiredTransactionsStatus(String initiativeId);

    long sendEventForStaleExpiredTransactions(String initiativeId);

}
