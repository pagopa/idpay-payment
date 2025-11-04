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
import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service("commonInvoice")
public class CommonInvoiceServiceImpl {

    private final long minDaysToInvoiceTransaction;
    private final TransactionInProgressRepository repository;
    private final TransactionNotifierService notifierService;
    private final PaymentErrorNotifierService paymentErrorNotifierService;
    private final FileStorageClient fileStorageClient;
    private final AuditUtilities auditUtilities;

    public CommonInvoiceServiceImpl(
            @Value("${app.common.expirations.minDaysToInvoiceTransaction:0}") long minDaysToInvoiceTransaction,
            TransactionInProgressRepository repository,
            TransactionNotifierService notifierService,
            PaymentErrorNotifierService paymentErrorNotifierService,
            FileStorageClient fileStorageClient,
            AuditUtilities auditUtilities) {
        this.minDaysToInvoiceTransaction = minDaysToInvoiceTransaction;
        this.repository = repository;
        this.notifierService = notifierService;
        this.paymentErrorNotifierService = paymentErrorNotifierService;
        this.fileStorageClient = fileStorageClient;
        this.auditUtilities = auditUtilities;
    }

    public void invoiceTransaction(String transactionId, String merchantId, String pointOfSaleId, MultipartFile file) {

        try {
            Utilities.checkFileExtensionOrThrow(file);

            // getting the transaction from transaction_in_progress and checking if it is valid for the invoiced status
            TransactionInProgress trx = repository.findById(transactionId)
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionId [%s]".formatted(transactionId)));
            if (!trx.getMerchantId().equals(merchantId)) {
                throw new TransactionInvalidException(ExceptionCode.GENERIC_ERROR, "The merchant with id [%s] associated to the transaction is not equal to the merchant with id [%s]".formatted(trx.getMerchantId(), merchantId));
            }
            if (!trx.getPointOfSaleId().equals(pointOfSaleId)) {
                throw new TransactionInvalidException(ExceptionCode.GENERIC_ERROR, "The pointOfSaleId with id [%s] associated to the transaction is not equal to the pointOfSaleId with id [%s]".formatted(trx.getPointOfSaleId(), pointOfSaleId));
            }
            if (!SyncTrxStatus.CAPTURED.equals(trx.getStatus())) {
                throw new OperationNotAllowedException(ExceptionCode.TRX_STATUS_NOT_VALID, "Cannot invoice transaction with status [%s], must be CAPTURED".formatted(trx.getStatus()));
            }
            // I want to invoice only transactions older than 'minDaysToInvoiceTransaction' days, minDaysToInvoiceTransaction default is 0
            if (minDaysToInvoiceTransaction > 0 && trx.getElaborationDateTime().plusDays(minDaysToInvoiceTransaction).isAfter(LocalDateTime.now())) {
                throw new OperationNotAllowedException(ExceptionCode.TRX_TOO_RECENT, "Cannot invoice transaction with elaboration date [%s], must be pass at least [%d] days".formatted(trx.getElaborationDateTime(), minDaysToInvoiceTransaction));
            }

            // Uploading invoice to storage
            String path = String.format("invoices/merchant/%s/pos/%s/transaction/%s/%s",
                    merchantId, pointOfSaleId, trx.getId(), file.getOriginalFilename());
            fileStorageClient.upload(file.getInputStream(), path, file.getContentType());

            // updating the transaction status to invoiced
            trx.setStatus(SyncTrxStatus.INVOICED);
            trx.setInvoiceFile(InvoiceFile.builder().filename(file.getOriginalFilename()).build());

            // sending the transaction invoice notification (to store it in transaction db collection)
            sendInvoiceTransactionNotification(trx);

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
            auditUtilities.logInvoiceTransaction(auditDTO);

            // removing the transaction from transaction_in_progress collection
            repository.deleteById(transactionId);

        } catch (RuntimeException e) {
            auditUtilities.logErrorInvoiceTransaction(transactionId, merchantId);
            throw e;
        } catch (IOException e) {
            auditUtilities.logErrorInvoiceTransaction(transactionId, merchantId);
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    private void sendInvoiceTransactionNotification(TransactionInProgress trx) {
        try {
            log.info("[INVOICE_TRANSACTION][SEND_NOTIFICATION] Sending Invoice Authorized Payment event to Notification: trxId {} - merchantId {}", trx.getId(), trx.getMerchantId());
            if (!notifierService.notify(trx, trx.getUserId())) {
                throw new InternalServerErrorException(ExceptionCode.GENERIC_ERROR, "Something gone wrong while invoicing Authorized Payment notify");
            }
        } catch (Exception e) {
            if (!paymentErrorNotifierService.notifyInvoicePayment(
                    notifierService.buildMessage(trx, trx.getUserId()),
                    "[INVOICE_TRANSACTION] An error occurred while publishing the invoice authorized result: trxId %s - merchantId %s".formatted(trx.getId(), trx.getMerchantId()),
                    true,
                    e)
            ) {
                log.error("[INVOICE_TRANSACTION][SEND_NOTIFICATION] An error has occurred and was not possible to notify it: trxId {} - merchantId {}", trx.getId(), trx.getUserId(), e);
            }
        }
    }
}