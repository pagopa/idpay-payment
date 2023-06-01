package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class QRCodeCancelServiceImpl implements QRCodeCancelService {

    private final TransactionInProgressRepository repository;
    private final RewardCalculatorConnector rewardCalculatorConnector;
    private final TransactionNotifierService notifierService;
    private final PaymentErrorNotifierService paymentErrorNotifierService;
    private final AuditUtilities auditUtilities;

    public QRCodeCancelServiceImpl(TransactionInProgressRepository repository,
                                   RewardCalculatorConnector rewardCalculatorConnector, TransactionNotifierService notifierService, PaymentErrorNotifierService paymentErrorNotifierService, AuditUtilities auditUtilities) {
        this.repository = repository;
        this.rewardCalculatorConnector = rewardCalculatorConnector;
        this.notifierService = notifierService;
        this.paymentErrorNotifierService = paymentErrorNotifierService;
        this.auditUtilities = auditUtilities;
    }

    @Override
    public void cancelTransaction(String trxId, String merchantId, String acquirerId) {
        try {
            TransactionInProgress trx = repository.findByIdThrottled(trxId);

            if (trx == null) {
                throw new ClientExceptionNoBody(HttpStatus.NOT_FOUND, "[CANCEL_TRANSACTION] Cannot found transaction having id: " + trxId);
            }
            if(!trx.getMerchantId().equals(merchantId) || !trx.getAcquirerId().equals(acquirerId)){
                throw new ClientExceptionNoBody(HttpStatus.FORBIDDEN, "[CANCEL_TRANSACTION] Requesting merchantId (%s through acquirer %s) not allowed to operate on transaction having id %s".formatted(merchantId, acquirerId, trxId));
            }

            if(SyncTrxStatus.REWARDED.equals(trx.getStatus())){
                throw new ClientExceptionNoBody(HttpStatus.BAD_REQUEST, "[CANCEL_TRANSACTION] Cannot cancel confirmed transaction: id %s".formatted(trxId));
            }

            if(!SyncTrxStatus.CREATED.equals(trx.getStatus())){
                AuthPaymentDTO refund = rewardCalculatorConnector.cancelTransaction(trx);
                if(refund != null) {
                    trx.setStatus(SyncTrxStatus.CANCELLED);
                    trx.setReward(refund.getReward());
                    trx.setRewards(refund.getRewards());
                    trx.setElaborationDateTime(LocalDateTime.now());

                    sendCancelledTransactionNotification(trx);
                }
            }

            log.info("[TRX_STATUS][CANCELLED] The transaction with trxId {} trxCode {}, has been cancelled", trx.getId(), trx.getTrxCode());
            repository.deleteById(trxId);

            auditUtilities.logCancelTransaction(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), ObjectUtils.firstNonNull(trx.getReward(), 0L), trx.getRejectionReasons(), merchantId);
        } catch (RuntimeException e) {
            auditUtilities.logErrorCancelTransaction(trxId, merchantId);
            throw e;
        }
    }

    private void sendCancelledTransactionNotification(TransactionInProgress trx) {
        try {
            log.info("[CANCEL_TRANSACTION][SEND_NOTIFICATION] Sending Cancel Authorized Payment event to Notification: trxId {} - merchantId {} - acquirerId {}", trx.getId(), trx.getMerchantId(), trx.getAcquirerId());
            if (!notifierService.notify(trx, trx.getUserId())) {
                throw new IllegalStateException("[CANCEL_TRANSACTION] Something gone wrong while cancelling Authorized Payment notify");
            }
        } catch (Exception e) {
            if(!paymentErrorNotifierService.notifyCancelPayment(
                    notifierService.buildMessage(trx, trx.getUserId()),
                    "[CANCEL_TRANSACTION] An error occurred while publishing the cancellation authorized result: trxId %s - merchantId %s - acquirerId %s".formatted(trx.getId(), trx.getMerchantId(), trx.getAcquirerId()),
                    true,
                    e)
            ) {
                log.error("[CANCEL_TRANSACTION][SEND_NOTIFICATION] An error has occurred and was not possible to notify it: trxId {} - merchantId {} - acquirerId {}", trx.getId(), trx.getUserId(), trx.getAcquirerId(), e);
            }
        }
    }

}
