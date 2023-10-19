package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.service.payment.common.CommonCancelServiceImpl;
import it.gov.pagopa.payment.service.payment.common.CommonCreationServiceImpl;
import it.gov.pagopa.payment.service.performancelogger.TransactionResponsePerfLoggerPayloadBuilder;
import it.gov.pagopa.payment.service.performancelogger.BaseTransactionResponseDTOPerfLoggerPayloadBuilder;
import it.gov.pagopa.payment.service.performancelogger.TransactionResponsePerfLoggerPayloadBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;
@Slf4j
@RestController
public class CommonPaymentControllerImpl implements CommonPaymentController {
    private final CommonCreationServiceImpl commonCreationService;
    private final CommonCancelServiceImpl commonCancelService;

    public CommonPaymentControllerImpl(@Qualifier("CommonCreate") CommonCreationServiceImpl commonCreationService,
                                       @Qualifier("CommonCancel")CommonCancelServiceImpl commonCancelService) {
        this.commonCreationService = commonCreationService;
        this.commonCancelService = commonCancelService;
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
}
