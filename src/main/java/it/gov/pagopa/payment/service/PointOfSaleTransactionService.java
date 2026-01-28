package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PointOfSaleTransactionService {
    Page<TransactionInProgress> getPointOfSaleTransactions(String merchantId,
                                                           String initiativeId,
                                                           String pointOfSaleId,
                                                           String fiscalCode,
                                                           TrxFiltersDTO filters,
                                                           Pageable pageable);
}
