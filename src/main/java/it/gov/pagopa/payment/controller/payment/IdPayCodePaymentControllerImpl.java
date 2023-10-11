package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.service.payment.IdpayCodePaymentService;
import it.gov.pagopa.payment.service.performancelogger.AuthPaymentDTOPerfLoggerPayloadBuilder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IdPayCodePaymentControllerImpl implements IdPayCodePaymentController {
    private final IdpayCodePaymentService idpayCodePaymentService;

    public IdPayCodePaymentControllerImpl(IdpayCodePaymentService idpayCodePaymentService) {
        this.idpayCodePaymentService = idpayCodePaymentService;
    }

    @Override
    @PerformanceLog(
            value = "IDPAYCODE_RELATE_USER",
            payloadBuilderBeanClass = AuthPaymentDTOPerfLoggerPayloadBuilder.class)
    public AuthPaymentDTO relateUser(String trxId, String userId) {
        return idpayCodePaymentService.relateUser(trxId,userId);
    }

    @Override
    @PerformanceLog(
            value = "IDPAYCODE_PREVIEW_TRANSACTION",
            payloadBuilderBeanClass = AuthPaymentDTOPerfLoggerPayloadBuilder.class)
    public AuthPaymentDTO previewPayment(String trxId, String userId) {
        return idpayCodePaymentService.previewPayment(trxId,userId);
    }

    @Override
    @PerformanceLog(
            value = "IDPAYCODE_AUTHORIZE_TRANSACTION",
            payloadBuilderBeanClass = AuthPaymentDTOPerfLoggerPayloadBuilder.class
    )
    public AuthPaymentDTO authPayment(String trxId, String merchantId, PinBlockDTO pinBlockbody) {
        return idpayCodePaymentService.authPayment(trxId,merchantId,pinBlockbody);
    }
}
