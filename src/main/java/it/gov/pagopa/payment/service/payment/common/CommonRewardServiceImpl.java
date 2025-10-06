package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.storage.FileStorageClient;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.TransactionAuditDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.*;
import it.gov.pagopa.payment.model.InvoiceFile;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.Utilities;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service("commonReward")
public class CommonRewardServiceImpl {

    private final TransactionInProgressRepository repository;
    private final TransactionNotifierService notifierService;
    private final PaymentErrorNotifierService paymentErrorNotifierService;
    private final FileStorageClient fileStorageClient;
    private final AuditUtilities auditUtilities;

    public CommonRewardServiceImpl(
            TransactionInProgressRepository repository,
            TransactionNotifierService notifierService,
            PaymentErrorNotifierService paymentErrorNotifierService,
            FileStorageClient fileStorageClient,
            AuditUtilities auditUtilities) {
        this.repository = repository;
        this.notifierService = notifierService;
        this.paymentErrorNotifierService = paymentErrorNotifierService;
        this.fileStorageClient = fileStorageClient;
        this.auditUtilities = auditUtilities;
    }

    public void rewardTransaction(String transactionId, String merchantId, String pointOfSaleId, MultipartFile file) {

        try {
            Utilities.checkFileExtensionOrThrow(file);

            // getting the transaction from transaction_in_progress and checking if it is valid for the reward
            TransactionInProgress trx = repository.findById(transactionId)
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionId [%s]".formatted(transactionId)));
            if (!trx.getMerchantId().equals(merchantId)) {
                throw new TransactionInvalidException(ExceptionCode.GENERIC_ERROR, "The merchant with id [%s] associated to the transaction is not equal to the merchant with id [%s]".formatted(trx.getMerchantId(), merchantId));
            }
            if (!trx.getPointOfSaleId().equals(pointOfSaleId)) {
                throw new TransactionInvalidException(ExceptionCode.GENERIC_ERROR, "The pointOfSaleId with id [%s] associated to the transaction is not equal to the pointOfSaleId with id [%s]".formatted(trx.getPointOfSaleId(), pointOfSaleId));
            }
            if (!SyncTrxStatus.CAPTURED.equals(trx.getStatus())) {
                throw new OperationNotAllowedException(ExceptionCode.TRX_STATUS_NOT_VALID, "Cannot reward transaction with status [%s], must be CAPTURED".formatted(trx.getStatus()));
            }

            // Uploading invoice to storage
            String path = String.format("invoices/merchant/%s/pos/%s/transaction/%s/%s",
                    merchantId, pointOfSaleId, trx.getId(), file.getOriginalFilename());
            fileStorageClient.upload(file.getInputStream(), path, file.getContentType());

            // updating the transaction status to rewarded
            trx.setStatus(SyncTrxStatus.REWARDED);
            trx.setInvoiceFile(InvoiceFile.builder().filename(file.getOriginalFilename()).build());

            // sending the transaction reward notification
            sendRewardTransactionNotification(trx);

            // logging operation
            TransactionAuditDTO auditDTO = new TransactionAuditDTO(
                    trx.getInitiativeId(),
                    trx.getId(),
                    trx.getTrxCode(),
                    trx.getUserId(),
                    ObjectUtils.firstNonNull(trx.getRewardCents(), 0L),
                    path,
                    merchantId,
                    pointOfSaleId
            );
            auditUtilities.logRewardTransaction(auditDTO);

            // removing the transaction from transaction_in_progress
            repository.deleteById(transactionId);

        } catch (RuntimeException e) {
            auditUtilities.logErrorRewardTransaction(transactionId, merchantId);
            throw e;
        } catch (IOException e) {
            auditUtilities.logErrorRewardTransaction(transactionId, merchantId);
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    private void sendRewardTransactionNotification(TransactionInProgress trx) {
        try {
            log.info("[REWARD_TRANSACTION][SEND_NOTIFICATION] Sending Reward Authorized Payment event to Notification: trxId {} - merchantId {}", trx.getId(), trx.getMerchantId());
            if (!notifierService.notify(trx, trx.getUserId())) {
                throw new InternalServerErrorException(ExceptionCode.GENERIC_ERROR, "Something gone wrong while rewarding Authorized Payment notify");
            }
        } catch (Exception e) {
            if (!paymentErrorNotifierService.notifyRewardPayment(
                    notifierService.buildMessage(trx, trx.getUserId()),
                    "[REWARD_TRANSACTION] An error occurred while publishing the reward authorized result: trxId %s - merchantId %s".formatted(trx.getId(), trx.getMerchantId()),
                    true,
                    e)
            ) {
                log.error("[REWARD_TRANSACTION][SEND_NOTIFICATION] An error has occurred and was not possible to notify it: trxId {} - merchantId {}", trx.getId(), trx.getUserId(), e);
            }
        }
    }
}