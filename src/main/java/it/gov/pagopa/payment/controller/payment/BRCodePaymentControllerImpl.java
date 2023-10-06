package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.brcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.dto.brcode.TransactionBRCodeCreationRequest;
import it.gov.pagopa.payment.dto.brcode.TransactionBRCodeResponse;
import it.gov.pagopa.payment.service.payment.BarCodePaymentService;
import it.gov.pagopa.payment.service.performancelogger.AuthPaymentDTOPerfLoggerPayloadBuilder;
import it.gov.pagopa.payment.service.performancelogger.TransactionResponsePerfLoggerPayloadBuilder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BRCodePaymentControllerImpl implements BRCodePaymentController{

    private final BarCodePaymentService barCodePaymentService;

    public BRCodePaymentControllerImpl(BarCodePaymentService barCodePaymentService){
        this.barCodePaymentService = barCodePaymentService;
    }

    @Override
    @PerformanceLog(
            value = "BR_CODE_CREATE_TRANSACTION",
            payloadBuilderBeanClass = TransactionResponsePerfLoggerPayloadBuilder.class)
    public TransactionBRCodeResponse createTransaction(TransactionBRCodeCreationRequest trxBRCodeCreationRequest, String userId) {
        return barCodePaymentService.createTransaction(trxBRCodeCreationRequest, userId);
    }

    @Override
    @PerformanceLog(
            value = "BR_CODE_AUTHORIZE_TRANSACTION",
            payloadBuilderBeanClass = AuthPaymentDTOPerfLoggerPayloadBuilder.class)
    public AuthPaymentDTO authPayment(String trxCode, AuthBarCodePaymentDTO authBarCodePaymentDTO, String merchantId) {
        return barCodePaymentService.authPayment(trxCode, authBarCodePaymentDTO.getAmountCents(), merchantId);
    }
}
