package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.TransactionSpecifications;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;


@Service
public class TransactionServiceImpl implements TransactionService {

    private static final List<SyncTrxStatus> DOWNLOADABLE_INVOICE_STATUSES = List.of(
            SyncTrxStatus.REWARDED,
            SyncTrxStatus.INVOICED,
            SyncTrxStatus.REFUNDED
    );

    private final TransactionRepository transactionRepository;
    private final PDVService pdvService;

    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            PDVService pdvService) {
        this.transactionRepository = transactionRepository;
        this.pdvService = pdvService;
    }

    @Override
    public Page<Transaction> getTransactionsByFilters(TrxFiltersDTO filters,
                                                      Pageable pageable) {
        Objects.requireNonNull(filters, "filters must not be null");
        Objects.requireNonNull(pageable, "pageable must not be null");

        return transactionRepository.findAll(TransactionSpecifications.buildSpecification(filters, encryptFiscalCode(filters.getFiscalCode())), pageable);
    }

    @Override
    public Transaction getTransactionByIdAndMerchantId(String transactionId,
                                                       String merchantId) {
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        Objects.requireNonNull(merchantId, "merchantId must not be null");

        return transactionRepository.findByIdAndMerchantIdAndStatusIn(
                        transactionId,
                        merchantId,
                        DOWNLOADABLE_INVOICE_STATUSES
                )
                .orElseThrow(() -> new TransactionNotFoundOrExpiredException(
                        "Cannot find transaction with transactionId [%s]".formatted(transactionId))
                );
    }

    @Override
    public Page<Transaction> getMerchantTransactionByFilter(TrxFiltersDTO filters, Pageable pageable){
        return transactionRepository.findAll(
                TransactionSpecifications.getFilters(filters, encryptFiscalCode(filters.getFiscalCode())),
                pageable
        );
    }

    private String encryptFiscalCode(String fiscalCode) {
        return StringUtils.isNotBlank(fiscalCode) ? pdvService.encryptCF(fiscalCode) : null;
    }

}
