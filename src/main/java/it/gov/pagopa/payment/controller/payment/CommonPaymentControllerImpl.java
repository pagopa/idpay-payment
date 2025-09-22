package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.common.mongo.retry.MongoRequestRateTooLargeApiRetryable;
import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.service.payment.common.*;
import it.gov.pagopa.payment.service.payment.expired.QRCodeExpirationService;
import it.gov.pagopa.payment.service.performancelogger.TransactionResponsePerfLoggerPayloadBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;
@Slf4j
@RestController
public class CommonPaymentControllerImpl implements CommonPaymentController {
    private final CommonCreationServiceImpl commonCreationService;
    private final CommonConfirmServiceImpl commonConfirmService;
    private final CommonCancelServiceImpl commonCancelService;
    private final CommonStatusTransactionServiceImpl commonStatusTransactionService;
    private final QRCodeExpirationService qrCodeExpirationService; // used just to force the expiration: this behavior is the same for all channels

    public CommonPaymentControllerImpl(@Qualifier("commonCreate") CommonCreationServiceImpl commonCreationService,
                                       @Qualifier("commonConfirm") CommonConfirmServiceImpl commonConfirmService,
                                       @Qualifier("commonCancel") CommonCancelServiceImpl commonCancelService,
                                       CommonStatusTransactionServiceImpl commonStatusTransactionService,
                                       QRCodeExpirationService qrCodeExpirationService) {
        this.commonCreationService = commonCreationService;
        this.commonConfirmService = commonConfirmService;
        this.commonCancelService = commonCancelService;
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
    @PerformanceLog(
            value = "CANCEL_TRANSACTION",
            payloadBuilderBeanClass = TransactionResponsePerfLoggerPayloadBuilder.class)
    public void cancelTransaction(String trxId, String merchantId, String acquirerId) {
        log.info(
                "[CANCEL_TRANSACTION] The merchant {} through acquirer {} is cancelling the transaction {}",
                merchantId,
                acquirerId,
                trxId);
        commonCancelService.cancelTransaction(trxId, merchantId, acquirerId);
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
