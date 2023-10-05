package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.brcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.brcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.service.payment.BarCodePaymentService;
import it.gov.pagopa.payment.service.performancelogger.TransactionResponsePerfLoggerPayloadBuilder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BarCodePaymentControllerImpl implements BarCodePaymentController {

    private final BarCodePaymentService barCodePaymentService;

    public BarCodePaymentControllerImpl(BarCodePaymentService barCodePaymentService){
        this.barCodePaymentService = barCodePaymentService;
    }

    @Override
    @PerformanceLog(
            value = "BAR_CODE_CREATE_TRANSACTION",
            payloadBuilderBeanClass = TransactionResponsePerfLoggerPayloadBuilder.class)
    public TransactionBarCodeResponse createTransaction(TransactionBarCodeCreationRequest trxBarCodeCreationRequest, String userId) {
        return barCodePaymentService.createTransaction(trxBarCodeCreationRequest, userId);
    }

    @Override
    @PerformanceLog(
            value = "BR_CODE_AUTHORIZE_TRANSACTION",
            payloadBuilderBeanClass = TransactionResponsePerfLoggerPayloadBuilder.class)
    public AuthPaymentDTO authPayment(String trxCode, String merchantId) {
        return barCodePaymentService.authPayment(trxCode, merchantId);
    }
}
