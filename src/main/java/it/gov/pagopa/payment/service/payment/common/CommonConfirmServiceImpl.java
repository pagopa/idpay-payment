package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.mapper.TransactionMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InternalServerErrorException;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service("commonConfirm")
public class CommonConfirmServiceImpl {
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final TransactionNotifierService notifierService;
    private final PaymentErrorNotifierService paymentErrorNotifierService;
    private final AuditUtilities auditUtilities;

    public CommonConfirmServiceImpl(TransactionRepository transactionRepository,
                                    TransactionMapper transactionMapper,
                                    TransactionNotifierService notifierService,
                                    PaymentErrorNotifierService paymentErrorNotifierService,
                                    AuditUtilities auditUtilities) {
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.notifierService = notifierService;
        this.paymentErrorNotifierService = paymentErrorNotifierService;
        this.auditUtilities = auditUtilities;
    }

    public TransactionResponse confirmPayment(String trxId, String merchantId, String acquirerId) {
        try {
            Transaction transaction = transactionRepository.findById(trxId)
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionId [%s]".formatted(trxId)));

            if(!SyncTrxStatus.AUTHORIZED.equals(transaction.getStatus())){
                throw new OperationNotAllowedException(PaymentConstants.ExceptionCode.TRX_OPERATION_NOT_ALLOWED,
                        "Cannot operate on transaction with transactionId [%s] in status %s".formatted(trxId,transaction.getStatus()));
            }
            if(!transaction.getMerchantId().equals(merchantId) || !transaction.getAcquirerId().equals(acquirerId)){
                throw new MerchantOrAcquirerNotAllowedException("The merchant with id [%s] associated to the transaction is not equal to the merchant with id [%s]".formatted(transaction.getMerchantId(), merchantId));
            }

            confirmAuthorizedPayment(transaction);

            auditUtilities.logConfirmedPayment(transaction.getInitiativeId(), transaction.getId(), transaction.getTrxCode(), transaction.getUserId(), transaction.getRewardCents(), transaction.getRejectionReasons(), transaction.getMerchantId());

            return transactionMapper.transactionToTransactionResponse(transaction);
        } catch (RuntimeException e) {
            auditUtilities.logErrorConfirmedPayment(trxId, merchantId);
            throw e;
        }
    }

    public void confirmAuthorizedPayment(Transaction transaction) {
        transaction.setStatus(SyncTrxStatus.REWARDED);
        transaction.setElaborationDateTime(LocalDateTime.now(ZoneId.of("Europe/Rome")));
        transaction.incrementTransactionRevision();
        log.info("[TRX_STATUS][REWARDED] The transaction with trxId {} trxCode {}, has been rewarded", transaction.getId(), transaction.getTrxCode());
        sendConfirmPaymentNotification(transaction);

        transactionRepository.save(transaction);
    }

    private void sendConfirmPaymentNotification(Transaction transaction) {
        try {
            log.info("[CONFIRM_PAYMENT][SEND_NOTIFICATION] Sending Confirmation Payment event to Notification: trxId {} - merchantId {} - acquirerId {}", transaction.getId(), transaction.getMerchantId(), transaction.getAcquirerId());
            if (!notifierService.notify(transaction, transaction.getMerchantId())) {
                throw new InternalServerErrorException(PaymentConstants.ExceptionCode.GENERIC_ERROR,  "Something gone wrong while Confirm Payment notify");
            }
        } catch (Exception e) {
            if(!paymentErrorNotifierService.notifyConfirmPayment(
                    notifierService.buildMessage(transaction, transaction.getMerchantId()),
                    "[CONFIRM_PAYMENT] An error occurred while publishing the confirmation Payment result: trxId %s - merchantId %s - acquirerId %s".formatted(transaction.getId(), transaction.getMerchantId(), transaction.getAcquirerId()),
                    true,
                    e)
            ) {
                log.error("[CONFIRM_PAYMENT][SEND_NOTIFICATION] An error has occurred and was not possible to notify it: trxId {} - merchantId {} - acquirerId {}", transaction.getId(), transaction.getUserId(), transaction.getAcquirerId(), e);
            }
        }
    }
}