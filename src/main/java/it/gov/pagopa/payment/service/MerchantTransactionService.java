package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.MerchantTransactionsListDTO;
import org.springframework.data.domain.Pageable;

public interface MerchantTransactionService {

    MerchantTransactionsListDTO getMerchantTransactions(String merchantId,
                                                        String initiativeId,
                                                        String fiscalCode,
                                                        String status, Pageable pageable);

    MerchantTransactionsListDTO getMerchantTransactionsProcessed(String merchantId,
                                                                 String organizationRole,
                                                                 String initiativeId,
                                                                 String fiscalCode,
                                                                 String status,
                                                                 String rewardBatchId,
                                                                 String rewardBatchTrxStatus,
                                                                 String pointOfSaleId,
                                                                 String trxCode,
                                                                 Pageable pageable);
}
