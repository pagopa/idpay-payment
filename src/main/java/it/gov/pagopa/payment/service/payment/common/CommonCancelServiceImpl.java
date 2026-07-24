package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.performancelogger.PerformanceLogger;
import it.gov.pagopa.common.utils.TransactionSynchronizer;
import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.CancelTransactionAuditDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InitiativeNotfoundException;
import it.gov.pagopa.payment.exception.custom.InternalServerErrorException;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.PointOfSaleNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.service.payment.barcode.BarCodeCreationServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import it.gov.pagopa.payment.utils.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static it.gov.pagopa.payment.constants.PaymentConstants.*;

@Slf4j
@Service("commonCancel")
public class CommonCancelServiceImpl {

    private final BarCodeCreationServiceImpl barCodeCreationService;
    private final TransactionRepository transactionRepository;
    private final TransactionInProgressRepository repository;
    private final RewardCalculatorConnector rewardCalculatorConnector;
    private final TransactionNotifierService notifierService;
    private final PaymentErrorNotifierService paymentErrorNotifierService;
    private final AuditUtilities auditUtilities;
    private final TransactionSynchronizer transactionSynchronizer;
    private final MerchantConnector merchantConnector;
    private static final String RESET_TRANSACTION = "RESET_TRANSACTION";
    private static final String CANCEL_TRANSACTION = "CANCEL_TRANSACTION";

    private static final String TRANSACTION_NOT_FOUND_MESSAGE =
            "Cannot find transaction with transactionId [%s]";

    public CommonCancelServiceImpl(
            TransactionRepository transactionRepository,
            TransactionInProgressRepository repository,
            RewardCalculatorConnector rewardCalculatorConnector,
            TransactionNotifierService notifierService,
            PaymentErrorNotifierService paymentErrorNotifierService,
            AuditUtilities auditUtilities,
            TransactionSynchronizer transactionSynchronizer,
            BarCodeCreationServiceImpl barCodeCreationService,
            MerchantConnector merchantConnector) {
        this.transactionRepository = transactionRepository;
        this.repository = repository;
        this.rewardCalculatorConnector = rewardCalculatorConnector;
        this.notifierService = notifierService;
        this.paymentErrorNotifierService = paymentErrorNotifierService;
        this.auditUtilities = auditUtilities;
        this.transactionSynchronizer = transactionSynchronizer;
        this.barCodeCreationService = barCodeCreationService;
        this.merchantConnector = merchantConnector;
    }

    public void cancelTransaction(String initiativeId, String trxId, String merchantId, String acquirerId, String pointOfSaleId) {
        try {
            log.info("[CANCEL_TRANSACTION] START - initiativeId={}, trxId={}, merchantId={}, acquirerId={}, pointOfSaleId={}",
                    Utilities.sanitizeString(initiativeId),
                    Utilities.sanitizeString(trxId),
                    Utilities.sanitizeString(merchantId),
                    Utilities.sanitizeString(acquirerId),
                    Utilities.sanitizeString(pointOfSaleId));

            TransactionInProgress trx = findAndValidateTransaction(initiativeId, trxId, merchantId, acquirerId, pointOfSaleId);

            if (isDeletableImmediately(trx)) {
                log.info("[CANCEL_TRANSACTION] BRANCH - immediate-delete status={}", trx.getStatus());
                repository.deleteById(trxId);

                if(!SyncTrxStatus.INVOICED.equals(trx.getStatus())){
                    trx.setStatus(SyncTrxStatus.CANCELLED);
                    Transaction transaction = new Transaction();
                    transactionSynchronizer.sync(trx, transaction);
                    transactionRepository.save(transaction);
                }
            } else if (SyncTrxStatus.AUTHORIZED.equals(trx.getStatus())) {
                log.info("[CANCEL_TRANSACTION] BRANCH - authorized cancellation trxId={}", Utilities.sanitizeString(trx.getId()));
                handleAuthorizedTransaction(trx);
            } else {
                throw new OperationNotAllowedException(ExceptionCode.TRX_DELETE_NOT_ALLOWED,
                        "Cannot cancel transaction with transactionId [%s]".formatted(trxId));
            }

            log.info("[TRX_STATUS][CANCELLED] The transaction with trxId {} trxCode {}, has been cancelled", trx.getId(), trx.getTrxCode());
            logCancelTransactionAudit(trx, merchantId, pointOfSaleId);

        } catch (RuntimeException e) {
            auditUtilities.logErrorCancelTransaction(trxId, merchantId);
            throw e;
        }
    }

    private TransactionInProgress findAndValidateTransaction(String initiativeId, String trxId, String merchantId, String acquirerId, String pointOfSaleId) {
        TransactionInProgress trx = repository.findById(trxId)
                .orElseThrow(() -> new TransactionNotFoundOrExpiredException(
                        TRANSACTION_NOT_FOUND_MESSAGE.formatted(trxId)));

        transactionRepository.findById(trxId)
                .orElseThrow(() -> new TransactionNotFoundOrExpiredException(
                        TRANSACTION_NOT_FOUND_MESSAGE.formatted(trxId)));

        if (!Objects.equals(merchantId, trx.getMerchantId()) || !Objects.equals(acquirerId, trx.getAcquirerId())) {
            throw new MerchantOrAcquirerNotAllowedException(
                    "The merchant with id [%s] associated to the transaction is not equal to the merchant with id [%s]"
                            .formatted(trx.getMerchantId(), merchantId));
        }
        validateTransactionContext(trx, initiativeId, pointOfSaleId);
        log.info("[CANCEL_TRANSACTION] Merchant/POS onboarding checks - merchantId={}, pointOfSaleId={}, initiativeId={}",
                Utilities.sanitizeString(merchantId),
                Utilities.sanitizeString(pointOfSaleId),
                Utilities.sanitizeString(initiativeId));
        merchantConnector.merchantDetail(merchantId, initiativeId);
        merchantConnector.getPointOfSale(merchantId, pointOfSaleId, initiativeId);
        log.info("[CANCEL_TRANSACTION] Checks Passed");
        return trx;
    }

    private void validateTransactionContext(TransactionInProgress trx, String initiativeId, String pointOfSaleId) {
        if (!Objects.equals(trx.getInitiativeId(), initiativeId)) {
            throw new InitiativeNotfoundException(
                    "The initiative with id [%s] associated to the transaction is not equal to the initiative with id [%s]"
                            .formatted(trx.getInitiativeId(), initiativeId));
        }
        if (!Objects.equals(trx.getPointOfSaleId(), pointOfSaleId)) {
            throw new PointOfSaleNotAllowedException(
                    "The pointOfSaleId with id [%s] associated to the transaction is not equal to the pointOfSaleId with id [%s]"
                            .formatted(trx.getPointOfSaleId(), pointOfSaleId));
        }
    }

    private boolean isDeletableImmediately(TransactionInProgress trx) {
        return SyncTrxStatus.CREATED.equals(trx.getStatus()) ||
                SyncTrxStatus.IDENTIFIED.equals(trx.getStatus()) ||
                SyncTrxStatus.INVOICED.equals(trx.getStatus());
    }

    private void handleAuthorizedTransaction(TransactionInProgress trx) {

        boolean isReset = trx.getExtendedAuthorization();
        log.info("[CANCEL_TRANSACTION] Reward cancellation call - trxId={}, reset={}",
                Utilities.sanitizeString(trx.getId()), isReset);
        AuthPaymentDTO refund = rewardCalculatorConnector.cancelTransaction(trx);

        repository.deleteById(trx.getId());

        if (refund != null) {
            trx.setStatus(SyncTrxStatus.CANCELLED);
            trx.setRewardCents(refund.getRewardCents());
            trx.setRewards(refund.getRewards());
            trx.setElaborationDateTime(LocalDateTime.now(ZoneOffset.UTC));

            Transaction transaction = new Transaction();
            transactionSynchronizer.sync(trx, transaction);
            transactionRepository.save(transaction);
            if (isReset) {
                TransactionInProgress newTrx = barCodeCreationService.createExtendedTransactionPostDelete(new TransactionBarCodeCreationRequest(trx.getInitiativeId(), trx.getVoucherAmountCents()),trx.getChannel(),trx.getUserId(),trx.getTrxEndDate());
                newTrx.setTrxCode(trx.getTrxCode());
                newTrx.setTrxDate(trx.getTrxDate());
                repository.save(newTrx);

                Transaction newTransaction = transactionRepository.findById(trx.getId())
                        .orElseThrow(() -> new TransactionNotFoundOrExpiredException(
                                TRANSACTION_NOT_FOUND_MESSAGE.formatted(trx.getId())));

                transactionSynchronizer.sync(newTrx, newTransaction);
                transactionRepository.save(newTransaction);
            }
            sendCancelledTransactionNotification(trx, isReset);
        }


    }

    private void logCancelTransactionAudit(TransactionInProgress trx, String merchantId, String pointOfSaleId) {
        CancelTransactionAuditDTO dto = new CancelTransactionAuditDTO(
                trx.getInitiativeId(),
                trx.getId(),
                trx.getTrxCode(),
                trx.getUserId(),
                ObjectUtils.firstNonNull(trx.getRewardCents(), 0L),
                trx.getRejectionReasons(),
                merchantId,
                pointOfSaleId
        );
        auditUtilities.logCancelTransaction(dto);
    }


    private void sendCancelledTransactionNotification(TransactionInProgress trx, boolean isReset) {
        try {
            log.info("[{}][SEND_NOTIFICATION] Sending Cancel Authorized Payment event to Notification: trxId {} - merchantId {} - acquirerId {}",
                    isReset ? RESET_TRANSACTION : CANCEL_TRANSACTION, trx.getId(), trx.getMerchantId(), trx.getAcquirerId());
            if (!notifierService.notify(trx, trx.getUserId())) {
                throw new InternalServerErrorException(ExceptionCode.GENERIC_ERROR, "Something gone wrong while cancelling Authorized Payment notify");
            }
        } catch (Exception e) {
            if (!paymentErrorNotifierService.notifyCancelPayment(
                    notifierService.buildMessage(trx, trx.getUserId()),
                    "[%s] An error occurred while publishing the cancellation authorized result: trxId %s - merchantId %s - acquirerId %s"
                            .formatted(isReset ? RESET_TRANSACTION : CANCEL_TRANSACTION, trx.getId(), trx.getMerchantId(), trx.getAcquirerId()),
                    true,
                    e)
            ) {
                log.error("[{}][SEND_NOTIFICATION] An error has occurred and was not possible to notify it: trxId {} - merchantId {} - acquirerId {}",
                        isReset ? RESET_TRANSACTION : CANCEL_TRANSACTION, trx.getId(), trx.getUserId(), trx.getAcquirerId(), e);
            }
        }
    }

    public void rejectPendingTransactions() {
        List<TransactionInProgress> transactions;
        int pageSize = 100;
        do {
            transactions = repository.findPendingTransactions(pageSize);
            log.info("[CANCEL_AUTHORIZED_TRANSACTIONS] Transactions to cancel: {} / {}", transactions.size(), pageSize);
            transactions.forEach(transaction ->
                    this.cancelTransaction(
                            transaction.getInitiativeId(),
                            transaction.getId(),
                            transaction.getMerchantId(),
                            transaction.getAcquirerId(),
                            transaction.getPointOfSaleId()));
        } while (!transactions.isEmpty());
    }

    public void deleteInvoicedTransaction() {
        while (true) {

            List<TransactionInProgress> batch =
                    fetchInvoicedTransaction();

            if (batch.isEmpty()) {
                log.debug("[{}] No more invoiced transactions found", INVOICED+RewardConstants.TRX_CHANNEL_QRCODE);
                break;
            }

            processBatchInvoiced(batch);
        }
    }

    private List<TransactionInProgress> fetchInvoicedTransaction() {
        return repository.findInvoicedTransaction(
                100
        );
    }

    private void processBatchInvoiced(List<TransactionInProgress> batch) {
        List<String> deletableIds = new ArrayList<>();

        for (TransactionInProgress trx : batch) {
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

            List<TransactionInProgress> batch =
                    fetchLapsedTransaction(initiativeId);

            if (batch.isEmpty()) {
                log.debug("[{}] No more expired transactions found", LAPSED+RewardConstants.TRX_CHANNEL_QRCODE);
                break;
            }

            processBatchLapsed(batch);
        }
    }

    private void processBatchLapsed(List<TransactionInProgress> batch) {

        List<String> deletableIds = new ArrayList<>();

        for (TransactionInProgress trx : batch) {
            processSingleTransaction(trx, deletableIds);
        }

        deleteProcessedTransactions(deletableIds);
    }

    private List<TransactionInProgress> fetchLapsedTransaction(String initiativeId) {
        return repository.findLapsedTransaction(
                initiativeId,
                100
        );
    }


    private void processSingleTransaction(TransactionInProgress trx, List<String> deletableIds) {
        logTransactionStart(trx);

        try {
            boolean canDelete = PerformanceLogger.execute(
                    LAPSED + RewardConstants.TRX_CHANNEL_QRCODE,
                    () -> handleExpiredTransactionBulk(trx),
                    _ -> "Evaluated transaction with ID %s due to DELETE_LAPSED_TRANSACTION"
                            .formatted(trx.getId())
            );

            if (canDelete) {
                deletableIds.add(trx.getId());
            }

            auditUtilities.logExpiredTransaction(
                    trx.getInitiativeId(),
                    trx.getId(),
                    trx.getTrxCode(),
                    trx.getUserId(),
                    DELETE_LAPSED_TRANSACTION
            );

        } catch (Exception e) {
            logAndAuditError(trx, e);
        }
    }

    private void logTransactionStart(TransactionInProgress trx) {
        log.info("[{}] [{}] Managing lapsed transaction trxId={}, status={}, trxDate={}",
                LAPSED+RewardConstants.TRX_CHANNEL_QRCODE,
                DELETE_LAPSED_TRANSACTION,
                trx.getId(),
                trx.getStatus(),
                trx.getTrxDate());
    }

    private void logAndAuditError(TransactionInProgress trx, Exception e) {
        log.error("[{}] [{}] Error handling transaction {}: {}",
                LAPSED+RewardConstants.TRX_CHANNEL_QRCODE,
                DELETE_LAPSED_TRANSACTION,
                trx.getId(),
                e.getMessage());

        auditUtilities.logErrorExpiredTransaction(
                trx.getInitiativeId(),
                trx.getId(),
                trx.getTrxCode(),
                trx.getUserId(),
                DELETE_LAPSED_TRANSACTION
        );
    }

    private void deleteProcessedTransactions(List<String> deletableIds) {
        if (!deletableIds.isEmpty()) {
            repository.bulkDeleteByIds(deletableIds);
            transactionRepository.bulkDeleteByIds(deletableIds);
        }
    }


    protected boolean handleExpiredTransactionBulk(TransactionInProgress trx) {
        if (SyncTrxStatus.IDENTIFIED.equals(trx.getStatus())) {
            try {
                rewardCalculatorConnector.cancelTransaction(trx);
            } catch (TransactionNotFoundOrExpiredException _) {
                log.debug("[{}] [{}] Transaction {} already expired, skipping cancel",
                        "LAPSED"+RewardConstants.TRX_CHANNEL_QRCODE,
                        DELETE_LAPSED_TRANSACTION,
                        trx.getId());
            } catch (ServiceException e) {
                log.warn("[{}] [{}] ServiceException cancelling transaction {}: {}",
                        LAPSED+RewardConstants.TRX_CHANNEL_QRCODE,
                        DELETE_LAPSED_TRANSACTION,
                        trx.getId(),
                        e.getMessage());
                return false;
            }
        }
        return true;
    }

}