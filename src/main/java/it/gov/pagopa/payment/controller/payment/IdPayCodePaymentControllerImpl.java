package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserRequest;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.service.payment.IdpayCodePaymentService;
import it.gov.pagopa.payment.service.performancelogger.AuthPaymentDTOPerfLoggerPayloadBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
@Slf4j
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
    public RelateUserResponse relateUser(String trxId, RelateUserRequest request) {
        log.info(
                "[IDPAYCODE_RELATE_USER] Request to relate user to transaction having transactionId {}",
                trxId);

        return idpayCodePaymentService.relateUser(trxId,request);
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
        log.info("[IDPAYCODE_AUTHORIZE_TRANSACTION] Request to authorize transaction with transactionId {}, by merchant having merchantId {}",trxId,merchantId);
        return idpayCodePaymentService.authPayment(trxId,merchantId,pinBlockbody);
    }
}
