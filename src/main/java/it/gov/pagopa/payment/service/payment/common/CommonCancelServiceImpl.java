package it.gov.pagopa.payment.service.payment.common;

import io.micrometer.common.util.StringUtils;
import it.gov.pagopa.common.performancelogger.PerformanceLogger;
import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.CancelTransactionAuditDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InternalServerErrorException;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.service.payment.barcode.BarCodeCreationServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static it.gov.pagopa.payment.constants.PaymentConstants.*;

@Slf4j
@Service("commonCancel")
public class CommonCancelServiceImpl {

    private static final String ZONE_EUROPE_ROME = "Europe/Rome";
    private final BarCodeCreationServiceImpl barCodeCreationService;
    private final TransactionRepository transactionRepository;
    private final RewardCalculatorConnector rewardCalculatorConnector;
    private final TransactionNotifierService notifierService;
    private final PaymentErrorNotifierService paymentErrorNotifierService;
    private final AuditUtilities auditUtilities;
    private static final String RESET_TRANSACTION = "RESET_TRANSACTION";
    private static final String CANCEL_TRANSACTION = "CANCEL_TRANSACTION";

    private static final String TRANSACTION_NOT_FOUND_MESSAGE =
            "Cannot find transaction with transactionId [%s]";

    public CommonCancelServiceImpl(
            TransactionRepository transactionRepository,
            RewardCalculatorConnector rewardCalculatorConnector,
            TransactionNotifierService notifierService,
            PaymentErrorNotifierService paymentErrorNotifierService,
            AuditUtilities auditUtilities,
            BarCodeCreationServiceImpl barCodeCreationService) {
        this.transactionRepository = transactionRepository;
        this.rewardCalculatorConnector = rewardCalculatorConnector;
        this.notifierService = notifierService;
        this.paymentErrorNotifierService = paymentErrorNotifierService;
        this.auditUtilities = auditUtilities;
        this.barCodeCreationService = barCodeCreationService;
    }

    public void cancelTransaction(String trxId, String merchantId, String acquirerId, String pointOfSaleId) {
        try {
            Transaction transaction = findAndValidateTransaction(trxId, merchantId, acquirerId);

            if (isDeletableImmediately(transaction)) {
                if (!SyncTrxStatus.INVOICED.equals(transaction.getStatus())) {
                    transaction.setStatus(SyncTrxStatus.CANCELLED);
                    transactionRepository.save(transaction);
                }
            } else if (SyncTrxStatus.AUTHORIZED.equals(transaction.getStatus())) {
                handleAuthorizedTransaction(transaction);
            } else {
                throw new OperationNotAllowedException(ExceptionCode.TRX_DELETE_NOT_ALLOWED,
                        "Cannot cancel transaction with transactionId [%s]".formatted(trxId));
            }

            log.info("[TRX_STATUS][CANCELLED] The transaction with trxId {} trxCode {}, has been cancelled", transaction.getId(), transaction.getTrxCode());
            logCancelTransactionAudit(transaction, merchantId, pointOfSaleId);

        } catch (RuntimeException e) {
            auditUtilities.logErrorCancelTransaction(trxId, merchantId);
            throw e;
        }
    }

    private Transaction findAndValidateTransaction(String trxId, String merchantId, String acquirerId) {
        Transaction transaction = transactionRepository.findById(trxId)
                .orElseThrow(() -> new TransactionNotFoundOrExpiredException(
                        TRANSACTION_NOT_FOUND_MESSAGE.formatted(trxId)));

        boolean merchantMismatch = StringUtils.isNotEmpty(merchantId) && !Objects.equals(merchantId, transaction.getMerchantId());
        boolean acquirerMismatch = !Objects.equals(acquirerId, transaction.getAcquirerId());

        if (merchantMismatch || acquirerMismatch) {
            throw new MerchantOrAcquirerNotAllowedException(
                    "The merchant with id [%s] associated to the transaction is not equal to the merchant with id [%s]"
                            .formatted(transaction.getMerchantId(), merchantId));
        }
        return transaction;
    }

    private boolean isDeletableImmediately(Transaction transaction) {
        return SyncTrxStatus.CREATED.equals(transaction.getStatus()) ||
                SyncTrxStatus.IDENTIFIED.equals(transaction.getStatus()) ||
                SyncTrxStatus.INVOICED.equals(transaction.getStatus());
    }

    private void handleAuthorizedTransaction(Transaction transaction) {

        boolean isReset = transaction.getExtendedAuthorization();
        AuthPaymentDTO refund = rewardCalculatorConnector.cancelTransaction(transaction);

        if (refund != null) {
            transaction.setStatus(SyncTrxStatus.CANCELLED);
            transaction.setRewardCents(refund.getRewardCents());
            transaction.setRewards(refund.getRewards());
            transaction.setElaborationDateTime(OffsetDateTime.now(ZoneId.of(ZONE_EUROPE_ROME)));
            transactionRepository.save(transaction);

            if (isReset) {
                Transaction newTrx = barCodeCreationService.createExtendedTransactionPostDelete(new TransactionBarCodeCreationRequest(transaction.getInitiativeId(), transaction.getVoucherAmountCents()), transaction.getChannel(), transaction.getUserId(), transaction.getTrxEndDate());
                newTrx.setTrxCode(transaction.getTrxCode());
                newTrx.setTrxDate(transaction.getTrxDate());
                transactionRepository.save(newTrx);
            }
            sendCancelledTransactionNotification(transaction, isReset);
        }


    }

    private void logCancelTransactionAudit(Transaction transaction, String merchantId, String pointOfSaleId) {
        CancelTransactionAuditDTO dto = new CancelTransactionAuditDTO(
                transaction.getInitiativeId(),
                transaction.getId(),
                transaction.getTrxCode(),
                transaction.getUserId(),
                ObjectUtils.firstNonNull(transaction.getRewardCents(), 0L),
                transaction.getRejectionReasons(),
                merchantId,
                pointOfSaleId
        );
        auditUtilities.logCancelTransaction(dto);
    }


    private void sendCancelledTransactionNotification(Transaction transaction, boolean isReset) {
        try {
            log.info("[{}][SEND_NOTIFICATION] Sending Cancel Authorized Payment event to Notification: trxId {} - merchantId {} - acquirerId {}",
                    isReset ? RESET_TRANSACTION : CANCEL_TRANSACTION, transaction.getId(), transaction.getMerchantId(), transaction.getAcquirerId());
            if (!notifierService.notify(transaction, transaction.getUserId())) {
                throw new InternalServerErrorException(ExceptionCode.GENERIC_ERROR, "Something gone wrong while cancelling Authorized Payment notify");
            }
        } catch (Exception e) {
            if (!paymentErrorNotifierService.notifyCancelPayment(
                    notifierService.buildMessage(transaction, transaction.getUserId()),
                    "[%s] An error occurred while publishing the cancellation authorized result: trxId %s - merchantId %s - acquirerId %s"
                            .formatted(isReset ? RESET_TRANSACTION : CANCEL_TRANSACTION, transaction.getId(), transaction.getMerchantId(), transaction.getAcquirerId()),
                    true,
                    e)
            ) {
                log.error("[{}][SEND_NOTIFICATION] An error has occurred and was not possible to notify it: trxId {} - merchantId {} - acquirerId {}",
                        isReset ? RESET_TRANSACTION : CANCEL_TRANSACTION, transaction.getId(), transaction.getUserId(), transaction.getAcquirerId(), e);
            }
        }
    }

    public void rejectPendingTransactions() {
        List<Transaction> transactions;
        int pageSize = 100;
        do {
            LocalDateTime threshold = LocalDateTime.now(ZoneId.of(ZONE_EUROPE_ROME)).minusHours(24);
            Pageable pageable = PageRequest.of(0, pageSize);
            transactions = transactionRepository.findByStatusAndUpdateDateBefore(
                    SyncTrxStatus.AUTHORIZED,
                    threshold,
                    pageable
            );
            log.info("[CANCEL_AUTHORIZED_TRANSACTIONS] Transactions to cancel: {} / {}", transactions.size(), pageSize);
            transactions.forEach(transaction ->
                    this.cancelTransaction(
                            transaction.getId(),
                            transaction.getMerchantId(),
                            transaction.getAcquirerId(),
                            transaction.getPointOfSaleId()));
        } while (!transactions.isEmpty());
    }

    public void deleteInvoicedTransaction() {
        while (true) {

            List<Transaction> batch =
                    fetchInvoicedTransaction();

            if (batch.isEmpty()) {
                log.debug("[{}] No more invoiced transactions found", INVOICED + RewardConstants.TRX_CHANNEL_QRCODE);
                break;
            }

            processBatchInvoiced(batch);
        }
    }

    private List<Transaction> fetchInvoicedTransaction() {
        Pageable pageable = PageRequest.of(0, 100);
        return transactionRepository.findByStatusOrderByTrxDateAsc(
                SyncTrxStatus.INVOICED,
                pageable
        );
    }

    private void processBatchInvoiced(List<Transaction> batch) {
        List<String> deletableIds = new ArrayList<>();

        for (Transaction trx : batch) {
            log.info("[{}] Managing expired transaction trxId={}, status={}, trxDate={}",
                    "DELETE_INVOICED_TRANSACTION",
                    trx.getId(),
                    trx.getStatus(),
                    trx.getTrxDate());
            deletableIds.add(trx.getId());
        }

        deleteProcessedTransactions(deletableIds);
    }


    public void deleteLapsedTransaction(String initiativeId) {
        while (true) {

            List<Transaction> batch =
                    fetchLapsedTransaction(initiativeId);

            if (batch.isEmpty()) {
                log.debug("[{}] No more expired transactions found", LAPSED + RewardConstants.TRX_CHANNEL_QRCODE);
                break;
            }

            processBatchLapsed(batch);
        }
    }

    private void processBatchLapsed(List<Transaction> batch) {

        List<String> deletableIds = new ArrayList<>();

        for (Transaction trx : batch) {
            processSingleTransaction(trx, deletableIds);
        }

        deleteProcessedTransactions(deletableIds);
    }

    private List<Transaction> fetchLapsedTransaction(String initiativeId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of(ZONE_EUROPE_ROME));
        Specification<Transaction> spec = (root, query, cb) -> {
            Predicate statusPredicate = root.get("status").in(
                    SyncTrxStatus.IDENTIFIED.name(),
                    SyncTrxStatus.CREATED.name(),
                    SyncTrxStatus.REJECTED.name()
            );

            Predicate datePredicate = cb.lessThan(root.get("trxEndDate"), now);

            Predicate extAuthPredicate = cb.or(
                    cb.notEqual(root.get("extendedAuthorization"), true),
                    cb.isNull(root.get("extendedAuthorization"))
            );


            return cb.and(statusPredicate, datePredicate, extAuthPredicate);
        };

        if (initiativeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("initiativeId"), initiativeId));
        }

        Pageable pageable = PageRequest.of(0, 100, Sort.by("trxDate").ascending());
        return transactionRepository.findAll(spec, pageable).stream().toList();
    }


    private void processSingleTransaction(Transaction transaction, List<String> deletableIds) {
        logTransactionStart(transaction);

        try {
            boolean canDelete = PerformanceLogger.execute(
                    LAPSED + RewardConstants.TRX_CHANNEL_QRCODE,
                    () -> handleExpiredTransactionBulk(transaction),
                    result -> "Evaluated transaction with ID %s due to DELETE_LAPSED_TRANSACTION"
                            .formatted(transaction.getId())
            );

            if (canDelete) {
                deletableIds.add(transaction.getId());
            }

            auditUtilities.logExpiredTransaction(
                    transaction.getInitiativeId(),
                    transaction.getId(),
                    transaction.getTrxCode(),
                    transaction.getUserId(),
                    DELETE_LAPSED_TRANSACTION
            );

        } catch (Exception e) {
            logAndAuditError(transaction, e);
        }
    }

    private void logTransactionStart(Transaction transaction) {
        log.info("[{}] [{}] Managing lapsed transaction trxId={}, status={}, trxDate={}",
                LAPSED + RewardConstants.TRX_CHANNEL_QRCODE,
                DELETE_LAPSED_TRANSACTION,
                transaction.getId(),
                transaction.getStatus(),
                transaction.getTrxDate());
    }

    private void logAndAuditError(Transaction transaction, Exception e) {
        log.error("[{}] [{}] Error handling transaction {}: {}",
                LAPSED + RewardConstants.TRX_CHANNEL_QRCODE,
                DELETE_LAPSED_TRANSACTION,
                transaction.getId(),
                e.getMessage());

        auditUtilities.logErrorExpiredTransaction(
                transaction.getInitiativeId(),
                transaction.getId(),
                transaction.getTrxCode(),
                transaction.getUserId(),
                DELETE_LAPSED_TRANSACTION
        );
    }

    private void deleteProcessedTransactions(List<String> deletableIds) {
        if (!deletableIds.isEmpty()) {
            transactionRepository.bulkDeleteByIds(deletableIds);
        }
    }


    protected boolean handleExpiredTransactionBulk(Transaction transaction) {
        if (SyncTrxStatus.IDENTIFIED.equals(transaction.getStatus())) {
            try {
                rewardCalculatorConnector.cancelTransaction(transaction);
            } catch (TransactionNotFoundOrExpiredException e) {
                log.debug("[{}] [{}] Transaction {} already expired, skipping cancel",
                        "LAPSED" + RewardConstants.TRX_CHANNEL_QRCODE,
                        DELETE_LAPSED_TRANSACTION,
                        transaction.getId());
            } catch (ServiceException e) {
                log.warn("[{}] [{}] ServiceException cancelling transaction {}: {}",
                        LAPSED + RewardConstants.TRX_CHANNEL_QRCODE,
                        DELETE_LAPSED_TRANSACTION,
                        transaction.getId(),
                        e.getMessage());
                return false;
            }
        }
        return true;
    }

}