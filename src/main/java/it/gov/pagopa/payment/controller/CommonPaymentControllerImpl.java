package it.gov.pagopa.payment.controller;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.service.performancelogger.TransactionResponsePerfLoggerPayloadBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
@Slf4j
@RestController
public class CommonPaymentControllerImpl implements CommonPaymentController {

    @Override
    @PerformanceLog(
            value = "CREATE_TRANSACTION",
            payloadBuilderBeanClass = TransactionResponsePerfLoggerPayloadBuilder.class)
    public TransactionResponse createTransaction(
            TransactionCreationRequest trxCreationRequest,
            String merchantId,
            String acquirerId,
            String idTrxIssuer) {
        log.info("[CREATE_TRANSACTION] The merchant {} through acquirer {} is creating a transaction", merchantId, acquirerId);
        return null; //TODO common service
    }
}
