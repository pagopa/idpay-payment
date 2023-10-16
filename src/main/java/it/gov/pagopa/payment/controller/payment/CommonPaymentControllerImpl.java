package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.common.BaseTransactionResponseDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.service.payment.common.CommonConfirmServiceImpl;
import it.gov.pagopa.payment.service.payment.common.CommonCreationServiceImpl;
import it.gov.pagopa.payment.service.performancelogger.BaseTransactionResponseDTOPerfLoggerPayloadBuilder;
import it.gov.pagopa.payment.service.performancelogger.TransactionResponsePerfLoggerPayloadBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;
@Slf4j
@RestController
public class CommonPaymentControllerImpl implements CommonPaymentController {
    private final CommonCreationServiceImpl commonCreationService;
    private final CommonConfirmServiceImpl commonConfirmService;

    public CommonPaymentControllerImpl(@Qualifier("CommonCreate") CommonCreationServiceImpl commonCreationService, CommonConfirmServiceImpl commonConfirmService) {
        this.commonCreationService = commonCreationService;
        this.commonConfirmService = commonConfirmService;
    }

    @Override
    @PerformanceLog(
            value = "CREATE_TRANSACTION",
            payloadBuilderBeanClass = BaseTransactionResponseDTOPerfLoggerPayloadBuilder.class)
    public BaseTransactionResponseDTO createTransaction(
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
}
