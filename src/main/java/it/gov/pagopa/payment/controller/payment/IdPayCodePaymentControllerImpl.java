package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserRequest;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.service.payment.IdpayCodePaymentService;
import it.gov.pagopa.payment.service.performancelogger.AuthPaymentDTOPerfLoggerPayloadBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    public RelateUserResponse relateUser(String trxId, RelateUserRequest request) {
        log.info(
                "[IDPAYCODE_RELATE_USER] Request to relate user to transaction having transactionId {}",
                trxId);
        try {
            return idpayCodePaymentService.relateUser(trxId,request);
        } catch (Exception e) {
            if(PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED.equals(e.getMessage())){
                throw new ClientExceptionWithBody(
                        HttpStatus.NOT_FOUND,
                        PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED,
                        "Cannot find transaction with transactionId [%s]".formatted(trxId));
            }
            throw e;
        }

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
