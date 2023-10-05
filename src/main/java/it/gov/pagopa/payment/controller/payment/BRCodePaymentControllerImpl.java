package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.brcode.TransactionBRCodeCreationRequest;
import it.gov.pagopa.payment.dto.brcode.TransactionBRCodeResponse;
import it.gov.pagopa.payment.service.payment.BRCodePaymentService;
import it.gov.pagopa.payment.service.performancelogger.TransactionResponsePerfLoggerPayloadBuilder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BRCodePaymentControllerImpl implements BRCodePaymentController{

    private final BRCodePaymentService brCodePaymentService;

    public BRCodePaymentControllerImpl(BRCodePaymentService brCodePaymentService){
        this.brCodePaymentService = brCodePaymentService;
    }

    @Override
    @PerformanceLog(
            value = "BR_CODE_CREATE_TRANSACTION",
            payloadBuilderBeanClass = TransactionResponsePerfLoggerPayloadBuilder.class)
    public TransactionBRCodeResponse createTransaction(TransactionBRCodeCreationRequest trxBRCodeCreationRequest, String userId) {
        return brCodePaymentService.createTransaction(trxBRCodeCreationRequest, userId);
    }

    @Override
    @PerformanceLog(
            value = "BR_CODE_AUTHORIZE_TRANSACTION",
            payloadBuilderBeanClass = TransactionResponsePerfLoggerPayloadBuilder.class)
    public AuthPaymentDTO authPayment(String trxCode, String merchantId) {
        return brCodePaymentService.authPayment(trxCode, merchantId);
    }
}
