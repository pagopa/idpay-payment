package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentRequestDTO;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.service.payment.BarCodePaymentService;
import it.gov.pagopa.payment.service.performancelogger.AuthPaymentDTOPerfLoggerPayloadBuilder;
import it.gov.pagopa.payment.service.performancelogger.PreviewPaymentDTOPerfLoggerPayloadBuilder;
import it.gov.pagopa.payment.service.performancelogger.TransactionBarCodeResponsePerfLoggerPayloadBuilder;
import it.gov.pagopa.payment.utils.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import static it.gov.pagopa.payment.utils.Utilities.sanitizeString;

@Slf4j
@RestController
public class BarCodePaymentControllerImpl implements BarCodePaymentController {

    private final BarCodePaymentService barCodePaymentService;

    public BarCodePaymentControllerImpl(BarCodePaymentService barCodePaymentService) {
        this.barCodePaymentService = barCodePaymentService;
    }

    @Override
    @PerformanceLog(
            value = "BAR_CODE_CREATE_TRANSACTION",
            payloadBuilderBeanClass = TransactionBarCodeResponsePerfLoggerPayloadBuilder.class)
    public TransactionBarCodeResponse createTransaction(TransactionBarCodeCreationRequest trxBarCodeCreationRequest, String userId) {
        log.info("[BAR_CODE_CREATE_TRANSACTION] The user {} is creating a transaction", Utilities.sanitizeString(userId));
        return barCodePaymentService.createTransaction(trxBarCodeCreationRequest, userId);
    }

    @Override
    @PerformanceLog(
            value = "BAR_CODE_AUTHORIZE_TRANSACTION",
            payloadBuilderBeanClass = AuthPaymentDTOPerfLoggerPayloadBuilder.class)
    public AuthPaymentDTO authPayment(String trxCode, AuthBarCodePaymentDTO authBarCodePaymentDTO, String merchantId, String pointOfSaleId, String acquirerId) {
        log.info("[BAR_CODE_AUTHORIZE_TRANSACTION] The merchant {} is authorizing the transaction having trxCode {}",
            Utilities.sanitizeString(merchantId), Utilities.sanitizeString(trxCode));
        return barCodePaymentService.authPayment(trxCode, authBarCodePaymentDTO, merchantId, pointOfSaleId, acquirerId);
    }

    @Override
    @PerformanceLog(
            value = "BAR_CODE_PREVIEW_PAYMENT",
            payloadBuilderBeanClass = PreviewPaymentDTOPerfLoggerPayloadBuilder.class)
    public PreviewPaymentDTO previewPayment(String trxCode, PreviewPaymentRequestDTO previewPaymentRequestDTO) {
        final String sanitizedTrxCode = sanitizeString(trxCode);
        final String sanitizedProductName = sanitizeString(previewPaymentRequestDTO.getProductName());
        final String sanitizedProductGtin = sanitizeString(previewPaymentRequestDTO.getProductGtin());
        final long amountCents = previewPaymentRequestDTO.getAmountCents().longValue();
        if (amountCents < 0L) {
            log.info("[PREVIEW_TRANSACTION] Cannot preview transaction with negative amountCents: {}", amountCents);
            throw new TransactionInvalidException(PaymentConstants.ExceptionCode.REWARD_NOT_VALID,
                    "Cannot preview transaction with negative amountCents [%s]".formatted(amountCents));
        }

        final PreviewPaymentDTO previewPaymentDTO = barCodePaymentService
                .previewPayment(sanitizedProductGtin, sanitizedTrxCode, amountCents);

        return previewPaymentDTO.withProductName(sanitizedProductName)
                .withProductGtin(sanitizedProductGtin);
    }

    @Override
    @PerformanceLog(
            value = "BAR_CODE_RETRIEVE_PAYMENT",
            payloadBuilderBeanClass = PreviewPaymentDTOPerfLoggerPayloadBuilder.class)
    public TransactionBarCodeResponse retrievePayment(String initiativeId, String userId) {
        return barCodePaymentService.findOldestNotAuthorized(userId, initiativeId);
    }

}
