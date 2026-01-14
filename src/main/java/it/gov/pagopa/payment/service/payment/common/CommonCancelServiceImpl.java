package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.performancelogger.PerformanceLogger;
import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.CancelTransactionAuditDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InternalServerErrorException;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.service.payment.barcode.BarCodeCreationServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service("commonCancel")
public class CommonCancelServiceImpl {

  private final BarCodeCreationServiceImpl barCodeCreationService;
    private final TransactionInProgressRepository repository;
    private final RewardCalculatorConnector rewardCalculatorConnector;
    private final TransactionNotifierService notifierService;
    private final PaymentErrorNotifierService paymentErrorNotifierService;
    private final AuditUtilities auditUtilities;
    private static final String RESET_TRANSACTION = "RESET_TRANSACTION";
    private static final String CANCEL_TRANSACTION = "CANCEL_TRANSACTION";


  public CommonCancelServiceImpl(
          TransactionInProgressRepository repository,
          RewardCalculatorConnector rewardCalculatorConnector,
          TransactionNotifierService notifierService,
          PaymentErrorNotifierService paymentErrorNotifierService,
          AuditUtilities auditUtilities,
          BarCodeCreationServiceImpl barCodeCreationService) {
        this.repository = repository;
        this.rewardCalculatorConnector = rewardCalculatorConnector;
        this.notifierService = notifierService;
        this.paymentErrorNotifierService = paymentErrorNotifierService;
        this.auditUtilities = auditUtilities;
    this.barCodeCreationService = barCodeCreationService;
  }

  public void cancelTransaction(String trxId, String merchantId, String acquirerId, String pointOfSaleId) {
    try {
      TransactionInProgress trx = findAndValidateTransaction(trxId, merchantId, acquirerId);

      if (isDeletableImmediately(trx)) {
        repository.deleteById(trxId);
      } else if (SyncTrxStatus.AUTHORIZED.equals(trx.getStatus())) {
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

  private TransactionInProgress findAndValidateTransaction(String trxId, String merchantId, String acquirerId) {
    TransactionInProgress trx = repository.findById(trxId)
        .orElseThrow(() -> new TransactionNotFoundOrExpiredException(
            "Cannot find transaction with transactionId [%s]".formatted(trxId)));

    if (!trx.getMerchantId().equals(merchantId) || !trx.getAcquirerId().equals(acquirerId)) {
      throw new MerchantOrAcquirerNotAllowedException(
          "The merchant with id [%s] associated to the transaction is not equal to the merchant with id [%s]"
              .formatted(trx.getMerchantId(), merchantId));
    }
    return trx;
  }

  private boolean isDeletableImmediately(TransactionInProgress trx) {
    return SyncTrxStatus.CREATED.equals(trx.getStatus()) || SyncTrxStatus.IDENTIFIED.equals(trx.getStatus());
  }

  private void handleAuthorizedTransaction(TransactionInProgress trx) {

    boolean isReset = trx.getExtendedAuthorization();
    AuthPaymentDTO refund = rewardCalculatorConnector.cancelTransaction(trx);

    repository.deleteById(trx.getId());
    if (refund != null) {
      trx.setStatus(SyncTrxStatus.CANCELLED);
      trx.setRewardCents(refund.getRewardCents());
      trx.setRewards(refund.getRewards());
      trx.setElaborationDateTime(LocalDateTime.now());

      if (isReset) {
        TransactionInProgress newTransaction = barCodeCreationService.createExtendedTransactionPostDelete(new TransactionBarCodeCreationRequest(trx.getInitiativeId(), trx.getVoucherAmountCents()),trx.getChannel(),trx.getUserId(),trx.getTrxEndDate());
        newTransaction.setTrxCode(trx.getTrxCode());
        newTransaction.setTrxDate(trx.getTrxDate());
        repository.save(newTransaction);
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
                            transaction.getId(),
                            transaction.getMerchantId(),
                            transaction.getAcquirerId(),
                            transaction.getPointOfSaleId()));
        } while (!transactions.isEmpty());
    }

    public void deleteLapsedTransaction(String initiativeId) {
      while (true) {

            List<TransactionInProgress> batch =
                    fetchLapsedTransaction(initiativeId);

            if (batch.isEmpty()) {
                log.debug("[{}] No more expired transactions found", "EXPIRED_"+RewardConstants.TRX_CHANNEL_QRCODE);
                break;
            }

            lockBatch(batch);
            processBatch(batch);
        }
    }
    private List<TransactionInProgress> fetchLapsedTransaction(
            String initiativeId) {

        return repository.findLapsedTransaction(
                initiativeId,
               100
        );
    }
    private void lockBatch(List<TransactionInProgress> batch) {
        repository.lockTransactions(batch);
    }

    private void processBatch(List<TransactionInProgress> batch) {

        List<String> deletableIds = new ArrayList<>();

        for (TransactionInProgress trx : batch) {
            processSingleTransaction(trx, deletableIds);
        }

        deleteProcessedTransactions(deletableIds);
    }
    private void processSingleTransaction(TransactionInProgress trx, List<String> deletableIds) {
        logTransactionStart(trx);

        try {
            boolean canDelete = PerformanceLogger.execute(
                    "EXPIRED_" + RewardConstants.TRX_CHANNEL_QRCODE,
                    () -> handleExpiredTransactionBulk(trx),
                    result -> "Evaluated transaction with ID %s due to TRANSACTION_AUTHORIZATION_EXPIRED"
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
                    "TRANSACTION_AUTHORIZATION_EXPIRED"
            );

        } catch (Exception e) {
            logAndAuditError(trx, e);
        }
    }

    private void logTransactionStart(TransactionInProgress trx) {
        log.info("[{}] [{}] Managing expired transaction trxId={}, status={}, trxDate={}",
                "EXPIRED_"+RewardConstants.TRX_CHANNEL_QRCODE,
                "TRANSACTION_AUTHORIZATION_EXPIRED",
                trx.getId(),
                trx.getStatus(),
                trx.getTrxDate());
    }

    private void logAndAuditError(TransactionInProgress trx, Exception e) {
        log.error("[{}] [{}] Error handling transaction {}: {}",
                "EXPIRED_"+RewardConstants.TRX_CHANNEL_QRCODE,
                "TRANSACTION_AUTHORIZATION_EXPIRED",
                trx.getId(),
                e.getMessage());

        auditUtilities.logErrorExpiredTransaction(
                trx.getInitiativeId(),
                trx.getId(),
                trx.getTrxCode(),
                trx.getUserId(),
                "TRANSACTION_AUTHORIZATION_EXPIRED"
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
                        "EXPIRED_"+RewardConstants.TRX_CHANNEL_QRCODE,
                        "TRANSACTION_AUTHORIZATION_EXPIRED",
                        trx.getId());
            } catch (ServiceException e) {
                log.warn("[{}] [{}] ServiceException cancelling transaction {}: {}",
                        "EXPIRED_"+RewardConstants.TRX_CHANNEL_QRCODE,
                        "TRANSACTION_AUTHORIZATION_EXPIRED",
                        trx.getId(),
                        e.getMessage());
                return false;
            }
        }
        return true;
    }


}
