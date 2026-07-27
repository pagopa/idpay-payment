package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.storage.FileStorageClient;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.RevertTransactionAuditDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InternalServerErrorException;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.InvoiceData;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service("commonReversal")
public class CommonReversalServiceImpl {

    private final TransactionRepository transactionRepository;
    private final TransactionNotifierService notifierService;
    private final PaymentErrorNotifierService paymentErrorNotifierService;
    private final FileStorageClient fileStorageClient;
    private final AuditUtilities auditUtilities;

    public CommonReversalServiceImpl(
            TransactionRepository transactionRepository,
            TransactionNotifierService notifierService,
            PaymentErrorNotifierService paymentErrorNotifierService,
            FileStorageClient fileStorageClient,
            AuditUtilities auditUtilities) {
        this.transactionRepository = transactionRepository;
        this.notifierService = notifierService;
        this.paymentErrorNotifierService = paymentErrorNotifierService;
        this.fileStorageClient = fileStorageClient;
        this.auditUtilities = auditUtilities;
    }

    public void reversalTransaction(String transactionId, String merchantId, String pointOfSaleId, MultipartFile file, String docNumber) {

        try {
            Utilities.checkFileExtensionOrThrow(file);

            // getting the transaction from transaction_in_progress and checking if it is valid for the reversal
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionId [%s]".formatted(transactionId)));

            if (!transaction.getMerchantId().equals(merchantId)) {
                throw new TransactionInvalidException(ExceptionCode.GENERIC_ERROR, "The merchant with id [%s] associated to the transaction is not equal to the merchant with id [%s]".formatted(transaction.getMerchantId(), merchantId));
            }
            if (!transaction.getPointOfSaleId().equals(pointOfSaleId)) {
                throw new TransactionInvalidException(ExceptionCode.GENERIC_ERROR, "The pointOfSaleId with id [%s] associated to the transaction is not equal to the pointOfSaleId with id [%s]".formatted(transaction.getPointOfSaleId(), pointOfSaleId));
            }
            if (!SyncTrxStatus.CAPTURED.equals(transaction.getStatus())) {
                throw new OperationNotAllowedException(ExceptionCode.TRX_STATUS_NOT_VALID, "Cannot reversal transaction with status [%s], must be CAPTURED".formatted(transaction.getStatus()));
            }

            // Uploading invoice to storage
            String path = String.format("invoices/merchant/%s/pos/%s/transaction/%s/creditNote/%s",
                    merchantId, pointOfSaleId, transaction.getId(), file.getOriginalFilename());
            fileStorageClient.upload(file.getInputStream(), path, file.getContentType());

            // updating the transaction
            transaction.setStatus(SyncTrxStatus.REFUNDED);
            transaction.setCreditNoteData(InvoiceData.builder()
                    .filename(file.getOriginalFilename())
                    .docNumber(docNumber)
                    .build());

            // sending the transaction reversal notification
            sendReversedTransactionNotification(transaction);

            // logging operation
            RevertTransactionAuditDTO auditDTO = new RevertTransactionAuditDTO(
                    transaction.getInitiativeId(),
                    transaction.getId(),
                    transaction.getTrxCode(),
                    transaction.getUserId(),
                    ObjectUtils.firstNonNull(transaction.getRewardCents(), 0L),
                    path,
                    docNumber,
                    merchantId,
                    pointOfSaleId
            );
            auditUtilities.logReverseTransaction(auditDTO);

            transactionRepository.save(transaction);

        } catch (RuntimeException e) {
            auditUtilities.logErrorReversalTransaction(transactionId, merchantId);
            throw e;
        } catch (IOException e) {
            auditUtilities.logErrorReversalTransaction(transactionId, merchantId);
            throw new InternalServerErrorException(ExceptionCode.GENERIC_ERROR, "Error uploading credit note file", false, e);
        }

    }

    private void sendReversedTransactionNotification(Transaction transaction) {
        try {
            log.info("[REVERSE_TRANSACTION][SEND_NOTIFICATION] Sending Reverse Authorized Payment event to Notification: trxId {} - merchantId {}", transaction.getId(), transaction.getMerchantId());
            if (!notifierService.notify(transaction, transaction.getUserId())) {
                throw new InternalServerErrorException(ExceptionCode.GENERIC_ERROR, "Something gone wrong while reversing Authorized Payment notify");
            }
        } catch (Exception e) {
            if (!paymentErrorNotifierService.notifyReversalPayment(
                    notifierService.buildMessage(transaction, transaction.getUserId()),
                    "[REVERSE_TRANSACTION] An error occurred while publishing the reversal authorized result: trxId %s - merchantId %s".formatted(transaction.getId(), transaction.getMerchantId()),
                    true,
                    e)
            ) {
                log.error("[REVERSE_TRANSACTION][SEND_NOTIFICATION] An error has occurred and was not possible to notify it: trxId {} - merchantId {}", transaction.getId(), transaction.getUserId(), e);
            }
        }
    }
}