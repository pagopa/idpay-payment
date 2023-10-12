package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.service.payment.IdpayCodePaymentService;
import it.gov.pagopa.payment.service.performancelogger.AuthPaymentDTOPerfLoggerPayloadBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class IdPayCodePaymentMilControllerImpl implements IdPayCodePaymentMilController {
    private final IdpayCodePaymentService idpayCodePaymentService;

    public IdPayCodePaymentMilControllerImpl(IdpayCodePaymentService idpayCodePaymentService) {
        this.idpayCodePaymentService = idpayCodePaymentService;
    }

    @Override
    @PerformanceLog(
            value = "IDPAYCODE_PREVIEW_TRANSACTION",
            payloadBuilderBeanClass = AuthPaymentDTOPerfLoggerPayloadBuilder.class)
    public AuthPaymentDTO previewPayment(String trxId, String merchantId) {
        log.info(
                "[IDPAYCODE_PREVIEW_TRANSACTION] The merchant {} request preview for transaction having transactionId {}",
                merchantId, trxId);
        return idpayCodePaymentService.previewPayment(trxId, merchantId);
    }

    @Override
    @PerformanceLog(
            value = "IDPAYCODE_AUTHORIZE_TRANSACTION",
            payloadBuilderBeanClass = AuthPaymentDTOPerfLoggerPayloadBuilder.class
    )
    public AuthPaymentDTO authPayment(String trxId, String userId) {
        return idpayCodePaymentService.authPayment(userId, trxId);
    }
}
