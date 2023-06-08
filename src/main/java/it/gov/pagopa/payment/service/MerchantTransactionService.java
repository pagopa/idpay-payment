package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.MerchantTransactionsListDTO;
import org.springframework.data.domain.Pageable;

public interface MerchantTransactionService {
    MerchantTransactionsListDTO getMerchantTransactions(String merchantId, String initiativeId, String fiscalCode, String status, Pageable pageable);
}
