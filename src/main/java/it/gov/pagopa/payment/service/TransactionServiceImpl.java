package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.TransactionSpecifications;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Objects;


@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final PDVService pdvService;

    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            PDVService pdvService) {
        this.transactionRepository = transactionRepository;
        this.pdvService = pdvService;
    }

    @Override
    public Page<Transaction> getTransactionsByFilters(TrxFiltersDTO filters, Pageable pageable) {
        Objects.requireNonNull(filters, "filters must not be null");
        Objects.requireNonNull(pageable, "pageable must not be null");

        String encryptedUserId = encryptFiscalCode(filters.getFiscalCode());
        Specification<Transaction> specification = buildSpecification(filters, encryptedUserId);

        return transactionRepository.findAll(specification, pageable);
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
}
