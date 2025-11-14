package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.common.mongo.retry.MongoRequestRateTooLargeApiRetryable;
import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.service.payment.common.*;
import it.gov.pagopa.payment.service.payment.expired.QRCodeExpirationService;
import it.gov.pagopa.payment.service.performancelogger.TransactionResponsePerfLoggerPayloadBuilder;
import it.gov.pagopa.payment.utils.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
public class CommonPaymentControllerImpl implements CommonPaymentController {
    private final CommonCreationServiceImpl commonCreationService;
    private final CommonConfirmServiceImpl commonConfirmService;
    private final CommonCancelServiceImpl commonCancelService;
    private final CommonCancelServiceBatchImpl commonCancelServiceBatch;
    private final CommonReversalServiceImpl commonReversalService;
    private final CommonInvoiceServiceImpl commonInvoiceService;
    private final CommonStatusTransactionServiceImpl commonStatusTransactionService;
    private final QRCodeExpirationService qrCodeExpirationService; // used just to force the expiration: this behavior is the same for all channels

    public CommonPaymentControllerImpl(@Qualifier("commonCreate") CommonCreationServiceImpl commonCreationService,
                                       @Qualifier("commonConfirm") CommonConfirmServiceImpl commonConfirmService,
                                       @Qualifier("commonCancel") CommonCancelServiceImpl commonCancelService,
                                       @Qualifier("commonCancelBatch") CommonCancelServiceBatchImpl commonCancelServiceBatch,
                                       @Qualifier("commonReversal") CommonReversalServiceImpl commonReversalService,
                                       @Qualifier("commonInvoice") CommonInvoiceServiceImpl commonInvoiceService,
                                       CommonStatusTransactionServiceImpl commonStatusTransactionService,
                                       QRCodeExpirationService qrCodeExpirationService) {
        this.commonCreationService = commonCreationService;
        this.commonConfirmService = commonConfirmService;
        this.commonCancelService = commonCancelService;
        this.commonCancelServiceBatch = commonCancelServiceBatch;
        this.commonReversalService = commonReversalService;
        this.commonInvoiceService = commonInvoiceService;
        this.commonStatusTransactionService = commonStatusTransactionService;
        this.qrCodeExpirationService = qrCodeExpirationService;
    }

    @Override
    @PerformanceLog(
            value = "CREATE_TRANSACTION",
            payloadBuilderBeanClass = TransactionResponsePerfLoggerPayloadBuilder.class)
    public  TransactionResponse createTransaction(
            TransactionCreationRequest trxCreationRequest,
            String merchantId,
            String acquirerId,
            String idTrxIssuer) {
        log.info("[CREATE_TRANSACTION] The merchant {} through acquirer {} is creating a transaction", merchantId, acquirerId);
        return commonCreationService.createTransaction(trxCreationRequest,null,merchantId,acquirerId,idTrxIssuer);
    }

    @Override
    @PerformanceLog(
            value = "CONFIRM_PAYMENT",
            payloadBuilderBeanClass = TransactionResponsePerfLoggerPayloadBuilder.class)
    public TransactionResponse confirmPayment(String trxId, String merchantId, String acquirerId) {
        log.info(
                "[CONFIRM_PAYMENT] The merchant {} through acquirer {} is confirming the transaction {}",
                merchantId,
                acquirerId,
                trxId);
        return commonConfirmService.confirmPayment(trxId, merchantId, acquirerId);
    }

    @Override
    @PerformanceLog(value = "CANCEL_TRANSACTION")
    public void cancelTransaction(String trxId, String merchantId, String acquirerId, String pointOfSaleId) {
        log.info(
                "[CANCEL_TRANSACTION] The merchant {} through acquirer {} is cancelling the transaction {} at POS {}",
                Utilities.sanitizeString(merchantId),
                Utilities.sanitizeString(acquirerId),
                Utilities.sanitizeString(trxId),
                Utilities.sanitizeString(pointOfSaleId)
        );
        commonCancelService.cancelTransaction(trxId, merchantId, acquirerId, pointOfSaleId);
    }

    @Override
    @PerformanceLog(value = "REVERSAL_TRANSACTION")
    public void reversalTransaction(String transactionId, String merchantId, String pointOfSaleId, MultipartFile file, String docNumber) {

        final String sanitizedMerchantId = Utilities.sanitizeString(merchantId);
        final String sanitizedTrxCode = Utilities.sanitizeString(transactionId);
        final String sanitizedPointOfSaleId = Utilities.sanitizeString(pointOfSaleId);

        log.info(
                "[REVERSAL_TRANSACTION] The merchant {} is requesting a reversal for the transactionId {} at POS {}",
                sanitizedMerchantId, sanitizedTrxCode, sanitizedPointOfSaleId
        );
        commonReversalService.reversalTransaction(transactionId, merchantId, pointOfSaleId, file, docNumber);
    }

    @Override
    @PerformanceLog(value = "INVOICE_TRANSACTION")
    public void invoiceTransaction(String transactionId, String merchantId, String pointOfSaleId, MultipartFile file, String docNumber) {

        final String sanitizedMerchantId = Utilities.sanitizeString(merchantId);
        final String sanitizedTrxCode = Utilities.sanitizeString(transactionId);
        final String sanitizedPointOfSaleId = Utilities.sanitizeString(pointOfSaleId);

        log.info(
            "[INVOICE_TRANSACTION] The merchant {} is requesting a invoice for the transactionId {} at POS {}",
            sanitizedMerchantId, sanitizedTrxCode, sanitizedPointOfSaleId
        );
        commonInvoiceService.invoiceTransaction(transactionId, merchantId, pointOfSaleId, file, docNumber);
    }

    @Override
    @PerformanceLog(value = "CANCEL_PENDING_TRANSACTIONS")
    public void cancelPendingTransactions() {
        log.info("[CANCEL_PENDING_TRANSACTIONS] Request to reject all transactions in AUTHORIZED status");
        commonCancelServiceBatch.rejectPendingTransactions();
    }

    @Override
    @PerformanceLog("GET_STATUS_TRANSACTION")
    public SyncTrxStatusDTO getStatusTransaction(String transactionId, String merchantId) {
        log.info("[GET_STATUS_TRANSACTION] The Merchant {} requested to retrieve status of transaction {} ", merchantId, transactionId);
        return commonStatusTransactionService.getStatusTransaction(transactionId, merchantId);
    }

    @Override
    @PerformanceLog("FORCE_CONFIRM_EXPIRATION")
    @MongoRequestRateTooLargeApiRetryable
    public Long forceConfirmTrxExpiration(String initiativeId) {
        log.info("[FORCE_CONFIRM_EXPIRATION] Requested confirm trx expiration for initiative {}", initiativeId);
        return qrCodeExpirationService.forceConfirmTrxExpiration(initiativeId);
    }

    @Override
    @PerformanceLog("FORCE_AUTHORIZATION_EXPIRATION")
    @MongoRequestRateTooLargeApiRetryable
    public Long forceAuthorizationTrxExpiration(String initiativeId) {
        log.info("[FORCE_AUTHORIZATION_EXPIRATION] Requested authorization trx expiration for initiative {}", initiativeId);
        return qrCodeExpirationService.forceAuthorizationTrxExpiration(initiativeId);
    }

}
