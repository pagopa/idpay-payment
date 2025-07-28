package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.PointOfSaleTransactionsListDTO;
import org.springframework.data.domain.Pageable;

public interface PointOfSaleTransactionService {
    PointOfSaleTransactionsListDTO getPointOfSaleTransactions(String merchantId, String initiativeId, String pointOfSaleId, String fiscalCode, String status, Pageable pageable);
}
