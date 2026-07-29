package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.DownloadInvoiceResponseDTO;
import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PointOfSaleTransactionService {

    Page<Transaction> getPointOfSaleTransactions(TrxFiltersDTO filters,
                                                 Pageable pageable);

    DownloadInvoiceResponseDTO downloadTransactionInvoice(
            String merchantId,
            String pointOfSaleId,
            String transactionId
    );

}
