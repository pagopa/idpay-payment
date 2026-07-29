package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.DownloadInvoiceResponseDTO;
import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface PointOfSaleTransactionService {

    Page<Transaction> getPointOfSaleTransactions(TrxFiltersDTO filters,
                                                 Pageable pageable);

    DownloadInvoiceResponseDTO downloadTransactionInvoice(
            String merchantId,
            String pointOfSaleId,
            String transactionId
    );

    void reversalTransaction(String transactionId, String merchantId, String pointOfSaleId, MultipartFile file, String docNumber);

    void updateInvoiceTransaction(String transactionId, String merchantId, String pointOfSaleId, MultipartFile file, String docNumber);

}
