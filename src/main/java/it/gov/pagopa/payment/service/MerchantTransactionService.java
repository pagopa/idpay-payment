package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.MerchantTransactionsListDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MerchantTransactionService {

    MerchantTransactionsListDTO getMerchantTransactions(
            String merchantId,
            String initiativeId,
            String fiscalCode,
            String status, Pageable pageable);

    MerchantTransactionsListDTO getMerchantTransactionsProcessed(
            String merchantId,
            String organizationRole,
            String initiativeId,
            String fiscalCode,
            List<String> statuses,
            String rewardBatchId,
            String rewardBatchTrxStatus,
            String pointOfSaleId,
            String trxCode,
            Pageable pageable);

    List<String> getProcessedTransactionStatuses(
            String organizationRole);
}
