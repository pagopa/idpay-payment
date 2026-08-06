package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.connector.storage.FileStorageClient;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.RevertTransactionAuditDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InitiativeNotfoundException;
import it.gov.pagopa.payment.exception.custom.InternalServerErrorException;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.InvoiceData;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.StoragePathUtils;
import it.gov.pagopa.payment.utils.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@Service("commonReversal")
public class CommonReversalServiceImpl {

    private final TransactionRepository transactionRepository;
    private final FileStorageClient fileStorageClient;
    private final AuditUtilities auditUtilities;

    public CommonReversalServiceImpl(
            TransactionRepository transactionRepository,
            FileStorageClient fileStorageClient,
            AuditUtilities auditUtilities) {
        this.transactionRepository = transactionRepository;
        this.fileStorageClient = fileStorageClient;
        this.auditUtilities = auditUtilities;
    }

    public void reversalTransaction(String initiativeId, String transactionId, String merchantId, MultipartFile file, String docNumber) {

        try {
            Utilities.checkFileExtensionOrThrow(file);

            // getting the transaction from transaction_in_progress and checking if it is valid for the reversal
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionId [%s]".formatted(transactionId)));

            if (!Objects.equals(transaction.getInitiativeId(), initiativeId)) {
                throw new InitiativeNotfoundException(
                        "The initiative with id [%s] associated to the transaction is not equal to the initiative with id [%s]"
                                .formatted(transaction.getInitiativeId(), initiativeId));
            }

            if (!transaction.getMerchantId().equals(merchantId)) {
                throw new TransactionInvalidException(ExceptionCode.GENERIC_ERROR, "The merchant with id [%s] associated to the transaction is not equal to the merchant with id [%s]".formatted(transaction.getMerchantId(), merchantId));
            }
            if(!(SyncTrxStatus.CAPTURED.equals(transaction.getStatus()) || (SyncTrxStatus.INVOICED.equals(transaction.getStatus()) && transaction.getInvoiceData() != null))) {
                throw new OperationNotAllowedException(ExceptionCode.TRX_STATUS_NOT_VALID, "Cannot reversal transaction with status [%s], must be CAPTURED".formatted(transaction.getStatus()));
            }

            // Uploading invoice to storage
            String path = StoragePathUtils.buildCreditNotePath(transaction, file.getOriginalFilename());
            fileStorageClient.upload(file.getInputStream(), path, file.getContentType());

            // updating the transaction
            transaction.setStatus(SyncTrxStatus.REFUNDED);
            transaction.setCreditNoteData(InvoiceData.builder()
                    .filename(file.getOriginalFilename())
                    .docNumber(docNumber)
                    .build());

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
                    transaction.getPointOfSaleId()
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

}