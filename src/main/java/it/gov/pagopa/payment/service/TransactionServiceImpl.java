package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TransactionMissingParametersException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.TransactionSpecifications;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode.TRANSACTIONS_MISSING_MANDATORY_FILTERS;
import static it.gov.pagopa.payment.constants.PaymentConstants.buildMissingFiltersMessage;

@Service
@Slf4j
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
    public Page<Transaction> getTransactionsByFilters(TrxFiltersDTO filters, Pageable pageable) {
        if (filters == null) {
            throw new TransactionMissingParametersException(
                    TRANSACTIONS_MISSING_MANDATORY_FILTERS,
                    buildMissingFiltersMessage("filters")
            );
        }
        String encryptedFiscalCode = encryptFiscalCode(filters.getFiscalCode());
        Specification<Transaction> spec = TransactionSpecifications.buildSpecification(filters, encryptedFiscalCode);
        return transactionRepository.findAll(spec, pageable);
    }

    @Override
    public Transaction getTransactionByIdAndMerchantId(String transactionId, String merchantId) {
        List<String> missingParams = new ArrayList<>();
        if (StringUtils.isBlank(transactionId)) {
            missingParams.add("transactionId");
        }
        if (StringUtils.isBlank(merchantId)) {
            missingParams.add("merchantId");
        }

        if (!missingParams.isEmpty()) {
            throw new TransactionMissingParametersException(
                    TRANSACTIONS_MISSING_MANDATORY_FILTERS,
                    buildMissingFiltersMessage(missingParams.toArray(new String[0]))
            );
        }

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
    public List<Transaction> findAll(
            String idTrxIssuer,
            String userId,
            LocalDateTime trxDateStart,
            LocalDateTime trxDateEnd,
            Long amountCents,
            Pageable pageable) {

        // Flusso 1: Ricerca per idTrxIssuer
        if (StringUtils.isNotBlank(idTrxIssuer)) {
            return findByIdTrxIssuer(idTrxIssuer, userId, trxDateStart, trxDateEnd, amountCents, pageable);
        }

        if (StringUtils.isNotBlank(userId) && trxDateStart != null && trxDateEnd != null) {
            return findByRange(userId, trxDateStart, trxDateEnd, amountCents, pageable);
        }

        List<String> missingFields = new ArrayList<>();
        if (StringUtils.isBlank(userId)) {
            missingFields.add("userId");
        }
        if (trxDateStart == null) {
            missingFields.add("trxDateStart");
        }
        if (trxDateEnd == null) {
            missingFields.add("trxDateEnd");
        }

        if (missingFields.size() == 3) {
            missingFields.addFirst("idTrxIssuer");
        }

        throw new TransactionMissingParametersException(
                TRANSACTIONS_MISSING_MANDATORY_FILTERS,
                buildMissingFiltersMessage(missingFields.toArray(new String[0]))
        );
    }

    @Override
    public List<Transaction> findByInitiativeIdAndUserId(String initiativeId, String userId) {
        List<String> missingParams = new ArrayList<>();
        if (StringUtils.isBlank(initiativeId)) {
            missingParams.add("initiativeId");
        }
        if (StringUtils.isBlank(userId)) {
            missingParams.add("userId");
        }

        if (!missingParams.isEmpty()) {
            throw new TransactionMissingParametersException(
                    TRANSACTIONS_MISSING_MANDATORY_FILTERS,
                    buildMissingFiltersMessage(missingParams.toArray(new String[0]))
            );
        }

        Specification<Transaction> spec = TransactionSpecifications.findByInitiativeAndUser(initiativeId, userId);
        return transactionRepository.findAll(spec);
    }

    @Override
    public Page<Transaction> getMerchantTransactionByFilter(TrxFiltersDTO filters, Pageable pageable) {
        String encryptedFiscalCode = encryptFiscalCode(filters.getFiscalCode());
        Specification<Transaction> spec = TransactionSpecifications.getFilters(filters, encryptedFiscalCode);
        return transactionRepository.findAll(spec, pageable);
    }

    private List<Transaction> findByIdTrxIssuer(
            String idTrxIssuer,
            String userId,
            LocalDateTime trxDateStart,
            LocalDateTime trxDateEnd,
            Long amountCents,
            Pageable pageable) {

        Specification<Transaction> spec = TransactionSpecifications.findByIssuerFilters(idTrxIssuer, userId, trxDateStart, trxDateEnd, amountCents);
        return transactionRepository.findAll(spec, pageable).getContent();
    }

    private List<Transaction> findByRange(
            String userId,
            LocalDateTime trxDateStart,
            LocalDateTime trxDateEnd,
            Long amountCents,
            Pageable pageable) {

        Specification<Transaction> spec = TransactionSpecifications.findByRangeFilters(userId, trxDateStart, trxDateEnd, amountCents);
        return transactionRepository.findAll(spec, pageable).getContent();
    }

    private String encryptFiscalCode(String fiscalCode) {
        return StringUtils.isNotBlank(fiscalCode) ? pdvService.encryptCF(fiscalCode) : null;
    }
}