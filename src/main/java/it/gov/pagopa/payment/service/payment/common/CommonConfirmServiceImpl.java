package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InternalServerErrorException;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service("commonConfirm")
public class CommonConfirmServiceImpl {
    private final TransactionInProgressRepository repository;
    private final TransactionInProgress2TransactionResponseMapper mapper;
    private final TransactionNotifierService notifierService;
    private final PaymentErrorNotifierService paymentErrorNotifierService;
    private final AuditUtilities auditUtilities;

    public CommonConfirmServiceImpl(TransactionInProgressRepository repository,
                                    TransactionInProgress2TransactionResponseMapper mapper,
                                    TransactionNotifierService notifierService, PaymentErrorNotifierService paymentErrorNotifierService, AuditUtilities auditUtilities) {
        this.repository = repository;
        this.mapper = mapper;
        this.notifierService = notifierService;
        this.paymentErrorNotifierService = paymentErrorNotifierService;
        this.auditUtilities = auditUtilities;
    }

    public TransactionResponse capturePayment(String trxCode) {
        try {
            TransactionInProgress trx = repository.findByTrxCode(trxCode)
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionCode [%s]".formatted(trxCode)));

            if(!trx.getStatus().equals(SyncTrxStatus.AUTHORIZED)){
                throw new OperationNotAllowedException(PaymentConstants.ExceptionCode.TRX_OPERATION_NOT_ALLOWED,
                        "Cannot operate on transaction with transactionCode [%s] in status %s".formatted(trxCode,trx.getStatus()));
            }

            trx.setStatus(SyncTrxStatus.CAPTURED);
            trx.setElaborationDateTime(LocalDateTime.now());
            repository.save(trx);

            auditUtilities.logCapturePayment(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), trx.getRewardCents(), trx.getRejectionReasons(), trx.getMerchantId());

            return mapper.apply(trx);
        } catch (RuntimeException e) {
            auditUtilities.logErrorCapturePayment(trxCode);
            throw e;
        }
    }


    public TransactionResponse confirmPayment(String trxId, String merchantId, String acquirerId) {
        try {
            TransactionInProgress trx = repository.findById(trxId)
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionId [%s]".formatted(trxId)));

            if(!SyncTrxStatus.AUTHORIZED.equals(trx.getStatus())){
                throw new OperationNotAllowedException(PaymentConstants.ExceptionCode.TRX_OPERATION_NOT_ALLOWED,
                        "Cannot operate on transaction with transactionId [%s] in status %s".formatted(trxId,trx.getStatus()));
            }
            if(!trx.getMerchantId().equals(merchantId) || !trx.getAcquirerId().equals(acquirerId)){
                throw new MerchantOrAcquirerNotAllowedException("The merchant with id [%s] associated to the transaction is not equal to the merchant with id [%s]".formatted(trx.getMerchantId(), merchantId));
            }

            confirmAuthorizedPayment(trx);

            auditUtilities.logConfirmedPayment(trx.getInitiativeId(), trx.getId(), trx.getTrxCode(), trx.getUserId(), trx.getRewardCents(), trx.getRejectionReasons(), trx.getMerchantId());

            return mapper.apply(trx);
        } catch (RuntimeException e) {
            auditUtilities.logErrorConfirmedPayment(trxId, merchantId);
            throw e;
        }
    }

    public void confirmAuthorizedPayment(TransactionInProgress trx) {
        trx.setStatus(SyncTrxStatus.REWARDED);
        trx.setElaborationDateTime(LocalDateTime.now());
        log.info("[TRX_STATUS][REWARDED] The transaction with trxId {} trxCode {}, has been rewarded", trx.getId(), trx.getTrxCode());
        sendConfirmPaymentNotification(trx);

        repository.deleteById(trx.getId());
    }

    private void sendConfirmPaymentNotification(TransactionInProgress trx) {
        try {
            log.info("[CONFIRM_PAYMENT][SEND_NOTIFICATION] Sending Confirmation Payment event to Notification: trxId {} - merchantId {} - acquirerId {}", trx.getId(), trx.getMerchantId(), trx.getAcquirerId());
            if (!notifierService.notify(trx, trx.getMerchantId())) {
                throw new InternalServerErrorException(PaymentConstants.ExceptionCode.GENERIC_ERROR,  "Something gone wrong while Confirm Payment notify");
            }
        } catch (Exception e) {
            if(!paymentErrorNotifierService.notifyConfirmPayment(
                    notifierService.buildMessage(trx, trx.getMerchantId()),
                    "[CONFIRM_PAYMENT] An error occurred while publishing the confirmation Payment result: trxId %s - merchantId %s - acquirerId %s".formatted(trx.getId(), trx.getMerchantId(), trx.getAcquirerId()),
                    true,
                    e)
            ) {
                log.error("[CONFIRM_PAYMENT][SEND_NOTIFICATION] An error has occurred and was not possible to notify it: trxId {} - merchantId {} - acquirerId {}", trx.getId(), trx.getUserId(), trx.getAcquirerId(), e);
            }
        }
    }
}
