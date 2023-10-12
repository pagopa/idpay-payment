package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.service.payment.IdpayCodePaymentService;
import it.gov.pagopa.payment.service.performancelogger.AuthPaymentDTOPerfLoggerPayloadBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class IdPayCodePaymentControllerImpl implements IdPayCodePaymentController {
    private final IdpayCodePaymentService idpayCodePaymentService;

    public IdPayCodePaymentControllerImpl(IdpayCodePaymentService idpayCodePaymentService) {
        this.idpayCodePaymentService = idpayCodePaymentService;
    }

    @Override
    @PerformanceLog(
            value = "IDPAYCODE_RELATE_USER",
            payloadBuilderBeanClass = AuthPaymentDTOPerfLoggerPayloadBuilder.class)
    public RelateUserResponse relateUser(String trxId, String fiscalCode) {
        log.info(
                "[IDPAYCODE_RELATE_USER] Request to relate user to transaction having transactionId {}",
                trxId);

        return idpayCodePaymentService.relateUser(trxId,fiscalCode);
    }
}
