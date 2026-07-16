package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.dto.CFDTO;
import it.gov.pagopa.payment.dto.EncryptedCfDTO;
import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.PDVInvocationException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.TransactionSpecifications;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    private final EncryptRestConnector encryptRestConnector;

    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            PDVService pdvService,
            EncryptRestConnector encryptRestConnector) {
        this.transactionRepository = transactionRepository;
        this.pdvService = pdvService;
        this.encryptRestConnector = encryptRestConnector;
    }

    @Override
    public Page<Transaction> getTransactionsByFilters(TrxFiltersDTO filters,
                                                      Pageable pageable) {
        Objects.requireNonNull(filters, "filters must not be null");
        Objects.requireNonNull(pageable, "pageable must not be null");

        String encryptedUserId = encryptFiscalCode(filters.getFiscalCode());
        Specification<Transaction> specification = buildSpecification(filters, encryptedUserId);

        return transactionRepository.findAll(specification, pageable);
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
        String userId = null;
        if (StringUtils.isNotBlank(filters.getFiscalCode())) {
            userId = encryptCF(filters.getFiscalCode());
        }

        Specification<Transaction> spec = TransactionSpecifications.getFilters(
                filters,
                userId
        );

        return transactionRepository.findAll(spec, pageable);
    }

    private String encryptFiscalCode(String fiscalCode) {
        return StringUtils.isNotBlank(fiscalCode) ? pdvService.encryptCF(fiscalCode) : null;
    }

    private Specification<Transaction> buildSpecification(TrxFiltersDTO filters, String encryptedUserId) {
        return Specification
                .where(TransactionSpecifications.hasStatus(filters.getStatus()))
                .and(TransactionSpecifications.hasTrxCode(filters.getTrxCode()))
                .and(TransactionSpecifications.hasMerchantId(filters.getMerchantId()))
                .and(TransactionSpecifications.hasInitiativeId(filters.getInitiativeId()))
                .and(TransactionSpecifications.hasFiscalCode(encryptedUserId))
                .and(TransactionSpecifications.hasRewardBatchId(filters.getRewardBatchId()))
                .and(TransactionSpecifications.hasRewardBatchTrxStatus(filters.getRewardBatchTrxStatus()))
                .and(TransactionSpecifications.hasPointOfSaleId(filters.getPointOfSaleId()))
                .and(TransactionSpecifications.hasProductGtin(filters.getProductGtin()));
    }

    private String encryptCF(String fiscalCode) {
        String userId;
        try {
            EncryptedCfDTO encryptedCfDTO = encryptRestConnector.upsertToken(new CFDTO(fiscalCode));
            userId = encryptedCfDTO.getToken();
        } catch (Exception e) {
            throw new PDVInvocationException("An error occurred during encryption",true,e);
        }
        return userId;
    }
}
