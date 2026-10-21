package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.MerchantTransactionsListDTO;
import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MerchantTransactionService {

    MerchantTransactionsListDTO getMerchantTransactions(
            String merchantId,
            String initiativeId,
            String fiscalCode,
            String status,
            String pointOfSaleId,
            Pageable pageable);

    MerchantTransactionsListDTO getMerchantTransactionsProcessed(
            String merchantId,
            String organizationRole,
            TrxFiltersDTO filters,
            Pageable pageable);

    List<String> getProcessedTransactionStatuses(
            String organizationRole);
}
