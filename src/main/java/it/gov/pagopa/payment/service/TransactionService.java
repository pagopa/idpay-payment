package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionService {

    Page<Transaction> getTransactionsByFilters(TrxFiltersDTO filters,
                                               Pageable pageable);

    Transaction getTransactionByIdAndMerchantId(String transactionId,
                                                String merchantId);
}
