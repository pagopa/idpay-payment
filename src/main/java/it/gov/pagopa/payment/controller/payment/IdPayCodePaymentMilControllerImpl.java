package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PinBlockDTO;
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
    public AuthPaymentDTO previewPayment(String trxId, String merchantId, String initiativeId) {
        log.info(
                "[IDPAYCODE_PREVIEW_TRANSACTION] The merchant {} request preview for transaction having transactionId {} on initiative {}",
                merchantId, trxId, initiativeId);
        return idpayCodePaymentService.previewPayment(trxId, merchantId, initiativeId);
    }

    @Override
    @PerformanceLog(
            value = "IDPAYCODE_AUTHORIZE_TRANSACTION",
            payloadBuilderBeanClass = AuthPaymentDTOPerfLoggerPayloadBuilder.class
    )
    public AuthPaymentDTO authPayment(String trxId, String merchantId, String initiativeId, PinBlockDTO pinBlockbody) { // <-- Aggiunto initiativeId
        log.info("[IDPAYCODE_AUTHORIZE_TRANSACTION] Request to authorize transaction with transactionId {}, by merchant having merchantId {} on initiative {}",
                trxId, merchantId, initiativeId);
        return idpayCodePaymentService.authPayment(trxId, merchantId, initiativeId, pinBlockbody);
    }
}
