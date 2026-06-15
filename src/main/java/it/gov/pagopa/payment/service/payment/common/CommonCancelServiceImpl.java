package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.performancelogger.PerformanceLogger;
import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.CancelTransactionAuditDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeCreationRequest2TransactionInProgressMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InternalServerErrorException;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.service.payment.barcode.BarCodeCreationServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.gov.pagopa.payment.constants.PaymentConstants.*;

@Slf4j
@Service("commonCancel")
public class CommonCancelServiceImpl {

    private final TransactionBarCodeCreationRequest2TransactionInProgressMapper transactionBarCodeCreationRequest2TransactionInProgressMapper;
    private final BarCodeCreationServiceImpl barCodeCreationService;
    private final TransactionRepository transactionRepository;
    private final TransactionInProgressRepository repository;
    private final RewardCalculatorConnector rewardCalculatorConnector;
    private final TransactionNotifierService notifierService;
    private final PaymentErrorNotifierService paymentErrorNotifierService;
    private final AuditUtilities auditUtilities;
    private static final String RESET_TRANSACTION = "RESET_TRANSACTION";
    private static final String CANCEL_TRANSACTION = "CANCEL_TRANSACTION";


    public CommonCancelServiceImpl(
            TransactionBarCodeCreationRequest2TransactionInProgressMapper transactionBarCodeCreationRequest2TransactionInProgressMapper,
            TransactionInProgressRepository repository,
            TransactionRepository transactionRepository,
            RewardCalculatorConnector rewardCalculatorConnector,
            TransactionNotifierService notifierService,
            PaymentErrorNotifierService paymentErrorNotifierService,
            AuditUtilities auditUtilities,
            BarCodeCreationServiceImpl barCodeCreationService) {
        this.transactionBarCodeCreationRequest2TransactionInProgressMapper = transactionBarCodeCreationRequest2TransactionInProgressMapper;
        this.repository = repository;
        this.transactionRepository = transactionRepository;
        this.rewardCalculatorConnector = rewardCalculatorConnector;
        this.notifierService = notifierService;
        this.paymentErrorNotifierService = paymentErrorNotifierService;
        this.auditUtilities = auditUtilities;
        this.barCodeCreationService = barCodeCreationService;
    }

    public void cancelTransaction(String trxId, String merchantId, String acquirerId, String pointOfSaleId) {
        try {
            TransactionInProgress mongo = findAndValidateTransactionInProgress(trxId, merchantId, acquirerId);
            Transaction postgres = findAndValidateTransaction(trxId, merchantId, acquirerId);

            if (isDeletableImmediately(mongo)) {
                repository.deleteById(trxId);
                transactionRepository.deleteById(trxId);
            } else if (SyncTrxStatus.AUTHORIZED.equals(mongo.getStatus())) {
                handleAuthorizedTransaction(mongo, postgres);
            } else {
                throw new OperationNotAllowedException(ExceptionCode.TRX_DELETE_NOT_ALLOWED,
                        "Cannot cancel transaction with transactionId [%s]".formatted(trxId));
            }

            log.info("[TRX_STATUS][CANCELLED] The transaction with trxId {} trxCode {}, has been cancelled", mongo.getId(), mongo.getTrxCode());
            logCancelTransactionAudit(mongo, merchantId, pointOfSaleId);

        } catch (RuntimeException e) {
            auditUtilities.logErrorCancelTransaction(trxId, merchantId);
            throw e;
        }
    }

    private TransactionInProgress findAndValidateTransactionInProgress(String trxId, String merchantId, String acquirerId) {
        TransactionInProgress trx = repository.findById(trxId)
                .orElseThrow(() -> new TransactionNotFoundOrExpiredException(
                        "Cannot find transaction with transactionId [%s]".formatted(trxId)));

        if (!merchantId.equals(trx.getMerchantId()) || !acquirerId.equals(trx.getAcquirerId())) {
            throw new MerchantOrAcquirerNotAllowedException(
                    "The merchant with id [%s] associated to the transaction is not equal to the merchant with id [%s]"
                            .formatted(trx.getMerchantId(), merchantId));
        }
        return trx;
    }

    private Transaction findAndValidateTransaction(String trxId, String merchantId, String acquirerId) {
        Transaction transaction = transactionRepository.findById(trxId)
                .orElseThrow(() -> new TransactionNotFoundOrExpiredException(
                        "Cannot find transaction with transactionId [%s]".formatted(trxId)));

        if (!merchantId.equals(transaction.getMerchantId()) || !acquirerId.equals(transaction.getAcquirerId())) {
            throw new MerchantOrAcquirerNotAllowedException(
                    "The merchant with id [%s] associated to the transaction is not equal to the merchant with id [%s]"
                            .formatted(transaction.getMerchantId(), merchantId));
        }
        return transaction;
    }

    private boolean isDeletableImmediately(TransactionInProgress trx) {
        return SyncTrxStatus.CREATED.equals(trx.getStatus()) ||
                SyncTrxStatus.IDENTIFIED.equals(trx.getStatus()) ||
                SyncTrxStatus.INVOICED.equals(trx.getStatus());
    }

    private void handleAuthorizedTransaction(TransactionInProgress mongo, Transaction postgres) {

        boolean isReset = mongo.getExtendedAuthorization();
        AuthPaymentDTO refund = rewardCalculatorConnector.cancelTransaction(mongo);

        repository.deleteById(mongo.getId());
        transactionRepository.deleteById(postgres.getId());
        if (refund != null) {
            mongo.setStatus(SyncTrxStatus.CANCELLED);
            mongo.setRewardCents(refund.getRewardCents());
            mongo.setRewards(refund.getRewards());
            mongo.setElaborationDateTime(LocalDateTime.now());

            postgres.setStatus(SyncTrxStatus.CANCELLED);
            postgres.setRewardCents(refund.getRewardCents());
            postgres.setRewards(refund.getRewards());
            postgres.setElaborationDate(LocalDateTime.now());

            if (isReset) {
                TransactionInProgress newTransaction = barCodeCreationService.createExtendedTransactionPostDelete(new TransactionBarCodeCreationRequest(mongo.getInitiativeId(), mongo.getVoucherAmountCents()),mongo.getChannel(),mongo.getUserId(),mongo.getTrxEndDate());
                newTransaction.setTrxCode(mongo.getTrxCode());
                newTransaction.setTrxDate(mongo.getTrxDate());
                repository.save(newTransaction);
                transactionRepository.save(transactionBarCodeCreationRequest2TransactionInProgressMapper.toPostgres(newTransaction, newTransaction.getTrxCode()));
            }
            sendCancelledTransactionNotification(mongo, isReset);
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
                            transaction.getId(),
                            transaction.getMerchantId(),
                            transaction.getAcquirerId(),
                            transaction.getPointOfSaleId()));
        } while (!transactions.isEmpty());

        List<Transaction> trxs;
        do {
            trxs = transactionRepository.findByStatusAndUpdateDateBefore(
                    SyncTrxStatus.AUTHORIZED,
                    LocalDateTime.now().minusHours(24),
                    PageRequest.of(0, pageSize)
            );
            log.info("[CANCEL_AUTHORIZED_TRANSACTIONS] Transactions to cancel: {} / {}", trxs.size(), pageSize);
            trxs.forEach(transaction ->
                    this.cancelTransaction(
                            transaction.getId(),
                            transaction.getMerchantId(),
                            transaction.getAcquirerId(),
                            transaction.getPointOfSaleId()));
        } while (!trxs.isEmpty());
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

    public void deleteInvoicedTransaction2() {
        while (true) {

            List<Transaction> batch = transactionRepository.findByStatusOrderByTrxDateAsc(
                    SyncTrxStatus.INVOICED,
                    PageRequest.of(0, 100)
            );

            if (batch.isEmpty()) {
                log.debug("[{}] No more invoiced transactions found", INVOICED+RewardConstants.TRX_CHANNEL_QRCODE);
                break;
            }

            processBatchInvoicedTransaction(batch);
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

    private void processBatchInvoicedTransaction(List<Transaction> batch) {
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

            List<TransactionInProgress> batch =
                    fetchLapsedTransaction(initiativeId);

            if (batch.isEmpty()) {
                log.debug("[{}] No more expired transactions found", LAPSED+RewardConstants.TRX_CHANNEL_QRCODE);
                break;
            }

            processBatchLapsed(batch);
        }


        while (true) {
            List<Transaction> batch = transactionRepository.findLapsedTransactions(
                    initiativeId,
                    OffsetDateTime.now(),
                    List.of(
                            SyncTrxStatus.IDENTIFIED,
                            SyncTrxStatus.CREATED,
                            SyncTrxStatus.REJECTED
                    ),
                    PageRequest.of(0, 100)
            );

            if (batch.isEmpty()) {
                log.debug("[{}] No more expired transactions found", LAPSED+RewardConstants.TRX_CHANNEL_QRCODE);
                break;
            }

            processTransactionBatchLapsed(batch);
        }
    }

    private void processBatchLapsed(List<TransactionInProgress> batch) {

        List<String> deletableIds = new ArrayList<>();

        for (TransactionInProgress trx : batch) {
            processSingleTransaction(trx, deletableIds);
        }

        deleteProcessedTransactions(deletableIds);
    }

    private void processTransactionBatchLapsed(List<Transaction> batch) {

        List<String> deletableIds = new ArrayList<>();

        for (Transaction trx : batch) {
            processSingleTransaction(trx, deletableIds);
        }

        if (!deletableIds.isEmpty()) {
            transactionRepository.bulkDeleteByIds(deletableIds);
        }
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
                    result -> "Evaluated transaction with ID %s due to DELETE_LAPSED_TRANSACTION"
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

    private void processSingleTransaction(Transaction trx, List<String> deletableIds) {
        logTransactionStart(trx);

        try {
            boolean canDelete = PerformanceLogger.execute(
                    LAPSED + RewardConstants.TRX_CHANNEL_QRCODE,
                    () -> handleExpiredTransactionBulk(trx),
                    result -> "Evaluated transaction with ID %s due to DELETE_LAPSED_TRANSACTION"
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

    private void logTransactionStart(Transaction trx) {
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

    private void logAndAuditError(Transaction trx, Exception e) {
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
        }
    }


    protected boolean handleExpiredTransactionBulk(TransactionInProgress trx) {
        if (SyncTrxStatus.IDENTIFIED.equals(trx.getStatus())) {
            try {
                rewardCalculatorConnector.cancelTransaction(trx);
            } catch (TransactionNotFoundOrExpiredException e) {
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

    protected boolean handleExpiredTransactionBulk(Transaction trx) {
        if (SyncTrxStatus.IDENTIFIED.equals(trx.getStatus())) {
            try {
                rewardCalculatorConnector.cancelTransaction(trx);
            } catch (TransactionNotFoundOrExpiredException e) {
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