package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.storage.FileStorageClient;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.ReversaInvoiceDTO;
import it.gov.pagopa.payment.dto.RevertTransactionAuditDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.*;
import it.gov.pagopa.payment.model.InvoiceFile;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service("commonCancel")
public class CommonReversalServiceImpl {

    private final TransactionInProgressRepository repository;
    private final TransactionNotifierService notifierService;
    private final PaymentErrorNotifierService paymentErrorNotifierService;
    private final FileStorageClient fileStorageClient;
    private final AuditUtilities auditUtilities;

    public CommonReversalServiceImpl(
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

    public void reversalTransaction(String trxCode, String merchantId, String pointOfSaleId, ReversaInvoiceDTO reversaInvoiceDTO) {

        try {
            // getting the transaction from transaction_in_progress and checking if it is valid for the reversal
            TransactionInProgress trx = repository.findById(trxCode)
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionId [%s]".formatted(trxCode)));
            if (!trx.getMerchantId().equals(merchantId)) {
                throw new TransactionInvalidException(ExceptionCode.GENERIC_ERROR, "The merchant with id [%s] associated to the transaction is not equal to the merchant with id [%s]".formatted(trx.getMerchantId(), merchantId));
            }
            if (!trx.getPointOfSaleId().equals(pointOfSaleId)) {
                throw new TransactionInvalidException(ExceptionCode.GENERIC_ERROR, "The pointOfSaleId with id [%s] associated to the transaction is not equal to the pointOfSaleId with id [%s]".formatted(trx.getPointOfSaleId(), pointOfSaleId));
            }
            if (!SyncTrxStatus.CAPTURED.equals(trx.getStatus())) {
                throw new OperationNotAllowedException(ExceptionCode.TRX_STATUS_NOT_VALID, "Cannot reversal transaction with status [%s], must be CAPTURED".formatted(trx.getStatus()));
            }

            // Uploading invoice to storage
            String path = String.format("INVOICES/merchant-%s/pos-%s/trxcode-%s/%s", merchantId, pointOfSaleId, trxCode, reversaInvoiceDTO.getFileName());
            fileStorageClient.upload(reversaInvoiceDTO.getFile().getInputStream(), path, reversaInvoiceDTO.getType());

            // updating the transaction
            trx.setStatus(SyncTrxStatus.REFUNDED);
            trx.setInvoiceFile(InvoiceFile.builder().filename(path).build());

            // sending the transaction reversal notification
            sendReversedTransactionNotification(trx);

            // logging operation
            RevertTransactionAuditDTO auditDTO = new RevertTransactionAuditDTO(
                    trx.getInitiativeId(),
                    trx.getId(),
                    trx.getTrxCode(),
                    trx.getUserId(),
                    ObjectUtils.firstNonNull(trx.getRewardCents(), 0L),
                    path,
                    merchantId,
                    pointOfSaleId
            );
            auditUtilities.logReverseTransaction(auditDTO);

            // removing the transaction from transaction_in_progress
            repository.deleteById(trxCode);

        } catch (RuntimeException e) {
            auditUtilities.logErrorReversalTransaction(trxCode, merchantId);
            throw e;
        } catch (IOException e) {
            auditUtilities.logErrorReversalTransaction(trxCode, merchantId);
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    private void sendReversedTransactionNotification(TransactionInProgress trx) {
        try {
            log.info("[REVERSE_TRANSACTION][SEND_NOTIFICATION] Sending Reverse Authorized Payment event to Notification: trxId {} - merchantId {}", trx.getId(), trx.getMerchantId());
            if (!notifierService.notify(trx, trx.getUserId())) {
                throw new InternalServerErrorException(ExceptionCode.GENERIC_ERROR, "Something gone wrong while reversing Authorized Payment notify");
            }
        } catch (Exception e) {
            if (!paymentErrorNotifierService.notifyCancelPayment(
                    notifierService.buildMessage(trx, trx.getUserId()),
                    "[REVERSE_TRANSACTION] An error occurred while publishing the reversal authorized result: trxId %s - merchantId %s".formatted(trx.getId(), trx.getMerchantId()),
                    true,
                    e)
            ) {
                log.error("[REVERSE_TRANSACTION][SEND_NOTIFICATION] An error has occurred and was not possible to notify it: trxId {} - merchantId {}", trx.getId(), trx.getUserId(), e);
            }
        }
    }
}