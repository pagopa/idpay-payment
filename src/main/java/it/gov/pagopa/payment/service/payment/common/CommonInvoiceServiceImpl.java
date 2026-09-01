package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.merchant.dto.PointOfSaleDTO;
import it.gov.pagopa.payment.connector.rest.rewardbatch.dto.RewardBatchEligibilityOperation;
import it.gov.pagopa.payment.connector.storage.FileStorageClient;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.TransactionAuditDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.enums.TransactionEventType;
import it.gov.pagopa.payment.exception.custom.InitiativeNotfoundException;
import it.gov.pagopa.payment.exception.custom.InternalServerErrorException;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.model.InvoiceData;
import it.gov.pagopa.payment.repository.InvoiceTransactionCommand;
import it.gov.pagopa.payment.repository.InvoiceTransactionRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.StoragePathUtils;
import it.gov.pagopa.payment.utils.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Slf4j
@Service("commonInvoice")
public class CommonInvoiceServiceImpl {

    private final long minDaysToInvoiceTransaction;
    private final TransactionRepository transactionRepository;
    private final FileStorageClient fileStorageClient;
    private final AuditUtilities auditUtilities;
    private final MerchantConnector merchantConnector;
    private final RewardBatchEligibilityPreflightService rewardBatchEligibilityPreflightService;
    private final InvoiceTransactionRepository invoiceTransactionRepository;

    public CommonInvoiceServiceImpl(
            @Value("${app.common.expirations.minDaysToInvoiceTransaction:0}") long minDaysToInvoiceTransaction,
            TransactionRepository transactionRepository,
            FileStorageClient fileStorageClient,
            AuditUtilities auditUtilities,
            MerchantConnector merchantConnector,
            RewardBatchEligibilityPreflightService rewardBatchEligibilityPreflightService,
            InvoiceTransactionRepository invoiceTransactionRepository) {
        this.minDaysToInvoiceTransaction = minDaysToInvoiceTransaction;
        this.transactionRepository = transactionRepository;
        this.fileStorageClient = fileStorageClient;
        this.auditUtilities = auditUtilities;
        this.merchantConnector = merchantConnector;
        this.rewardBatchEligibilityPreflightService = rewardBatchEligibilityPreflightService;
        this.invoiceTransactionRepository = invoiceTransactionRepository;
    }

    public void invoiceTransaction(
            String initiativeId,
            String transactionId,
            String merchantId,
            String authorization,
            MultipartFile file,
            String docNumber) {

        try {
            Utilities.checkFileExtensionOrThrow(file);

            // getting the transaction from transaction_in_progress and checking if it is valid for the invoiced status
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

            boolean isInvoicedOrRewarded = SyncTrxStatus.INVOICED.equals(transaction.getStatus())
                    || SyncTrxStatus.REWARDED.equals(transaction.getStatus());
            if (!(SyncTrxStatus.CAPTURED.equals(transaction.getStatus())
                    || (isInvoicedOrRewarded && transaction.getInvoiceData() != null))) {
                throw new OperationNotAllowedException(
                        ExceptionCode.TRX_STATUS_NOT_VALID,
                        "Cannot invoice transaction with status [%s], must be CAPTURED, INVOICED or REWARDED"
                                .formatted(transaction.getStatus()));
            }

            // I want to invoice only transactions older than 'minDaysToInvoiceTransaction' days, minDaysToInvoiceTransaction default is 0
            if (transaction.getInvoiceData() == null && (minDaysToInvoiceTransaction > 0 && transaction.getElaborationDateTime().plusDays(minDaysToInvoiceTransaction).isAfter(LocalDateTime.now(ZoneId.of("Europe/Rome"))))) {
                throw new OperationNotAllowedException(ExceptionCode.TRX_TOO_RECENT, "Cannot invoice transaction with elaboration date [%s], must be pass at least [%d] days".formatted(transaction.getElaborationDateTime(), minDaysToInvoiceTransaction));
            }

            if (isInvoicedOrRewarded) {
                rewardBatchEligibilityPreflightService.verifyEligibility(
                        transaction,
                        RewardBatchEligibilityOperation.INVOICE_REPLACEMENT,
                        authorization);
            }

            SyncTrxStatus expectedStatus = transaction.getStatus();
            long expectedRevision = transaction.getTransactionRevision() == null
                    ? 0L
                    : transaction.getTransactionRevision();
            TransactionEventType eventType = SyncTrxStatus.CAPTURED.equals(expectedStatus)
                    ? TransactionEventType.TRANSACTION_INVOICED
                    : TransactionEventType.TRANSACTION_INVOICE_REPLACED;

            InvoiceData oldDocumentData = transaction.getInvoiceData();
            if(oldDocumentData!=null){
                String oldFilename = oldDocumentData.getFilename();
                String oldBlobPath = StoragePathUtils.buildInvoicePath(transaction, oldFilename);
                fileStorageClient.deleteFile(oldBlobPath);
            }

            // Uploading invoice to storage
            String path = StoragePathUtils.buildInvoicePath(transaction, file.getOriginalFilename());
            fileStorageClient.upload(file.getInputStream(), path, file.getContentType());

            LocalDateTime updateDate = LocalDateTime.now(ZoneId.of("Europe/Rome"));
            InvoiceData newInvoiceData = InvoiceData.builder()
                    .filename(file.getOriginalFilename())
                    .docNumber(docNumber)
                    .build();
            String franchiseName = transaction.getFranchiseName();
            String pointOfSaleType = transaction.getPointOfSaleType();
            String businessName = transaction.getBusinessName();
            String merchantFiscalCode = transaction.getMerchantFiscalCode();

            if (oldDocumentData == null && (transaction.getFranchiseName() == null || transaction.getPointOfSaleType() == null)) {
                PointOfSaleDTO pointOfSaleDTO = merchantConnector.getPointOfSale(merchantId, transaction.getPointOfSaleId());

                franchiseName = pointOfSaleDTO.getFranchiseName();
                pointOfSaleType = pointOfSaleDTO.getType().name();
                businessName = pointOfSaleDTO.getBusinessName();
                merchantFiscalCode = pointOfSaleDTO.getFiscalCode();
            }
            invoiceTransactionRepository.updateInvoiceAndCreateEvent(new InvoiceTransactionCommand(
                    transaction.getId(),
                    transaction.getInitiativeId(),
                    transaction.getMerchantId(),
                    expectedStatus,
                    expectedRevision,
                    newInvoiceData,
                    updateDate,
                    franchiseName,
                    pointOfSaleType,
                    businessName,
                    merchantFiscalCode,
                    eventType));
            TransactionAuditDTO auditDTO = new TransactionAuditDTO(
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
            if (TransactionEventType.TRANSACTION_INVOICED.equals(eventType)) {
                auditUtilities.logInvoiceTransaction(auditDTO);
            } else {
                auditUtilities.logInvoiceReplacement(auditDTO);
            }

        } catch (RuntimeException e) {
            auditUtilities.logErrorInvoiceTransaction(transactionId, merchantId);
            throw e;
        } catch (IOException e) {
            auditUtilities.logErrorInvoiceTransaction(transactionId, merchantId);
            throw new InternalServerErrorException(ExceptionCode.GENERIC_ERROR, "Error uploading invoice file", false, e);
        }

    }

    public void invoiceTransaction(String initiativeId, String transactionId, String merchantId, MultipartFile file, String docNumber) {
        invoiceTransaction(initiativeId, transactionId, merchantId, null, file, docNumber);
    }

}