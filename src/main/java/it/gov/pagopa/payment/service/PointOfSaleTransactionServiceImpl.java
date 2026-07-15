package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Objects;


@Service
public class PointOfSaleTransactionServiceImpl implements PointOfSaleTransactionService {

    private final TransactionService transactionService;

    public PointOfSaleTransactionServiceImpl(
            TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Override
    public Page<Transaction> getPointOfSaleTransactions(TrxFiltersDTO filters,
                                                        Pageable pageable) {
        Objects.requireNonNull(filters, "filters must not be null");
        Objects.requireNonNull(pageable, "pageable must not be null");
        return transactionService.getTransactionsByFilters(filters, pageable);
    }
}
