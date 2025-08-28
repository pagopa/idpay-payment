package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentRequestDTO;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.service.payment.BarCodePaymentService;
import it.gov.pagopa.payment.service.performancelogger.AuthPaymentDTOPerfLoggerPayloadBuilder;
import it.gov.pagopa.payment.service.performancelogger.TransactionBarCodeResponsePerfLoggerPayloadBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

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
        log.info("[BAR_CODE_CREATE_TRANSACTION] The user {} is creating a transaction", userId);
        return barCodePaymentService.createTransaction(trxBarCodeCreationRequest, userId);
    }

    @Override
    @PerformanceLog(
            value = "BAR_CODE_AUTHORIZE_TRANSACTION",
            payloadBuilderBeanClass = AuthPaymentDTOPerfLoggerPayloadBuilder.class)
    public AuthPaymentDTO authPayment(String trxCode, AuthBarCodePaymentDTO authBarCodePaymentDTO, String merchantId, String acquirerId) {
        log.info("[BAR_CODE_AUTHORIZE_TRANSACTION] The merchant {} is authorizing the transaction having trxCode {}", merchantId, trxCode);
        return barCodePaymentService.authPayment(trxCode, authBarCodePaymentDTO, merchantId, acquirerId);
    }

    @Override
    @PerformanceLog(
            value = "BAR_CODE_PREVIEW_PAYMENT",
            payloadBuilderBeanClass = AuthPaymentDTOPerfLoggerPayloadBuilder.class)
    public PreviewPaymentDTO previewPayment(PreviewPaymentRequestDTO previewPaymentRequestDTO) {
        log.info("[BAR_CODE_PREVIEW_PAYMENT] Retrieve preview payment having trxCode {}", previewPaymentRequestDTO.getTrxCode());
        PreviewPaymentDTO previewPaymentDTO = barCodePaymentService.previewPayment(previewPaymentRequestDTO.getTrxCode());
        previewPaymentDTO.setProduct(previewPaymentRequestDTO.getProduct());
        return previewPaymentDTO;
    }

}
