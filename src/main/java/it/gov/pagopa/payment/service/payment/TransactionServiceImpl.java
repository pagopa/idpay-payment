package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.configuration.AppConfigurationProperties;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.dto.TrxFiltersDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.ExpirationStatusUpdateException;
import it.gov.pagopa.payment.exception.custom.TransactionMissingParametersException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.PDVService;
import it.gov.pagopa.payment.utils.TransactionSpecifications;
import it.gov.pagopa.payment.utils.TrxCodeGenUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode.TRANSACTIONS_MISSING_MANDATORY_FILTERS;
import static it.gov.pagopa.payment.constants.PaymentConstants.buildMissingFiltersMessage;
import static it.gov.pagopa.payment.utils.Utilities.sanitizeForLog;

@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private static final List<SyncTrxStatus> DOWNLOADABLE_INVOICE_STATUSES = List.of(
            SyncTrxStatus.REWARDED,
            SyncTrxStatus.INVOICED,
            SyncTrxStatus.REFUNDED
    );

    private static final ZoneId ZONE_EUROPE_ROME = ZoneId.of("Europe/Rome");

    private final TransactionRepository transactionRepository;
    private final PDVService pdvService;
    private final TrxCodeGenUtil trxCodeGenUtil;
    private final AppConfigurationProperties.ExtendedTransactions extendedTransactions;
    private final TransactionNotifierService transactionNotifierService;
    private final AppConfigurationProperties.ExtendedTransactions appConfigurationProperties;



    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            PDVService pdvService,
            TrxCodeGenUtil trxCodeGenUtil,
            AppConfigurationProperties.ExtendedTransactions extendedTransactions,
            TransactionNotifierService transactionNotifierService,
            AppConfigurationProperties.ExtendedTransactions appConfigurationProperties) {
        this.transactionRepository = transactionRepository;
        this.pdvService = pdvService;
        this.trxCodeGenUtil = trxCodeGenUtil;
        this.extendedTransactions = extendedTransactions;
        this.transactionNotifierService = transactionNotifierService;
        this.appConfigurationProperties = appConfigurationProperties;
    }

    @Override
    public void generateTrxCodeAndSave(Transaction transaction, String flowName) {
        long retry = 1;
        String trxCode;
        while(true){
            trxCode = trxCodeGenUtil.get();
            if(!transactionRepository.existsByTrxCode(trxCode)){
                break;
            }
            log.info(
                    "[{}] [GENERATE_TRX_CODE] Duplicate hit: generating new trxCode [Retry #{}]",
                    flowName,
                    retry);
        }
        transaction.setTrxCode(trxCode);

        transactionRepository.save(transaction);
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

    @Override
    public long findAndUpdateExpiredTransactionsStatus(String initiativeId) {
        try {
            OffsetDateTime now = OffsetDateTime.now(ZONE_EUROPE_ROME);
            log.info("[BATCH_EXPIRED_VOUCHER] Starting expiration update for initiative: {}", sanitizeForLog(initiativeId));
            int updatedRows = transactionRepository.updateStatusForExpiredVoucherTransactions(initiativeId, now);

            log.info("[BATCH_EXPIRED_VOUCHER] Updated expired vouchers directly in DB: {}", updatedRows);
            return updatedRows;
        } catch (Exception e) {
            log.error("[UPDATE_EXPIRED_TRANSACTIONS_STATUS] Encountered an error during the update of the existing " +
                    "transactions for which trx status is expired, with message {}", e.getMessage(), e);
            throw new ExpirationStatusUpdateException(e.getMessage());
        }
    }

    @Override
    public long sendEventForStaleExpiredTransactions(String initiativeId) {
        Integer page = 0;
        long numberOfEvents = 0L;
        try {
            while (true) {
                OffsetDateTime threshold = OffsetDateTime.now(ZONE_EUROPE_ROME)
                        .minusMinutes(extendedTransactions.getStaleMinutesThreshold());
                Pageable pageable = Pageable.ofSize(appConfigurationProperties.getSendExpiredSendBatchSize()).withPage(page);

                List<Transaction> transactionList = transactionRepository
                        .findByInitiativeIdAndStatusAndUpdateDateBeforeAndExtendedAuthorizationIsTrueOrderByIdAsc(
                                initiativeId,
                                SyncTrxStatus.EXPIRED,
                                threshold,
                                pageable
                        );

                numberOfEvents = numberOfEvents + transactionList.size();
                transactionList.parallelStream().forEach(
                        transaction -> {
                            if (!transactionNotifierService.notify(
                                    transaction, transaction.getId())) {
                                log.error("[SEND_EVENT_FOR_STALE_EXPIRED_TRX] Unable to send trx with id {}",
                                        transaction.getId());
                                throw new ExpirationStatusUpdateException("Unable to send trx with id " +
                                        transaction.getId());
                            }
                        });

                if (transactionList.isEmpty() ||
                        transactionList.size() < appConfigurationProperties.getSendExpiredSendBatchSize()) {
                    log.info(
                            "[SEND_EVENT_FOR_STALE_EXPIRED_TRX] Successfully sent {} stale transactions in EXPIRED state" +
                                    "left unprocessed for recovery", numberOfEvents);
                    return numberOfEvents;
                }

                page++;

            }

        } catch (ExpirationStatusUpdateException expirationStatusUpdateException) {
            throw expirationStatusUpdateException;
        } catch (Exception e) {
            log.error("[SEND_EVENT_TRANSACTIONS_STATUS] Encountered an error during the process: {}",
                    e.getMessage(), e);
            throw new ExpirationStatusUpdateException(e.getMessage());
        }

    }

    @Override
    public int updateTransactionsStatus(List<String> transactionIds, SyncTrxStatus status) {
        List<String> validIds = transactionIds.stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();

        if (validIds.isEmpty()) {
            throw new TransactionMissingParametersException(
                    TRANSACTIONS_MISSING_MANDATORY_FILTERS,
                    buildMissingFiltersMessage("transactionIds")
            );
        }

        return transactionRepository.bulkUpdateStatusByIds(
                validIds,
                status,
                LocalDateTime.now(ZONE_EUROPE_ROME)
        );
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