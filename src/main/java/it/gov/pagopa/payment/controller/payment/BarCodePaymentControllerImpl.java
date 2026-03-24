package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentRequestDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentRequestV2DTO;
import it.gov.pagopa.payment.dto.PreviewPaymentResponseV2DTO;
import it.gov.pagopa.payment.dto.PreviewPaymentResultDTO;
import it.gov.pagopa.payment.dto.ReportDTO;
import it.gov.pagopa.payment.dto.ReportDTOWithTrxCode;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.service.payment.BarCodePaymentService;
import it.gov.pagopa.payment.service.pdf.PdfService;
import it.gov.pagopa.payment.service.performancelogger.AuthPaymentDTOPerfLoggerPayloadBuilder;
import it.gov.pagopa.payment.service.performancelogger.PreviewPaymentDTOPerfLoggerPayloadBuilder;
import it.gov.pagopa.payment.service.performancelogger.PreviewPaymentResponseV2DTOPerfLoggerPayloadBuilder;
import it.gov.pagopa.payment.service.performancelogger.TransactionBarCodeResponsePerfLoggerPayloadBuilder;
import it.gov.pagopa.payment.utils.Utilities;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import static it.gov.pagopa.payment.utils.Utilities.sanitizeString;

@Slf4j
@RestController
public class BarCodePaymentControllerImpl implements BarCodePaymentController {

    private static final String PRODUCT_NAME_KEY = "productName";
    private static final String PRODUCT_GTIN_KEY = "productGtin";

    private final BarCodePaymentService barCodePaymentService;
    private final PdfService pdfService;

    public BarCodePaymentControllerImpl(BarCodePaymentService barCodePaymentService, PdfService pdfService) {
        this.barCodePaymentService = barCodePaymentService;
        this.pdfService = pdfService;
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
        final long amountCents = validatePreviewAmount(previewPaymentRequestDTO.getAmountCents());

        final PreviewPaymentResultDTO previewPaymentResult = barCodePaymentService
                .previewPayment(sanitizedTrxCode, buildLegacyPreviewAdditionalProperties(sanitizedProductName, sanitizedProductGtin), amountCents);

        return mapPreviewPaymentV1(previewPaymentResult, sanitizedProductName, sanitizedProductGtin);
    }

    @Override
    @PerformanceLog(
            value = "BAR_CODE_PREVIEW_PAYMENT",
            payloadBuilderBeanClass = PreviewPaymentResponseV2DTOPerfLoggerPayloadBuilder.class)
    public PreviewPaymentResponseV2DTO previewPaymentV2(String trxCode, PreviewPaymentRequestV2DTO previewPaymentRequestV2DTO) {
        final String sanitizedTrxCode = sanitizeString(trxCode);
        final long amountCents = validatePreviewAmount(previewPaymentRequestV2DTO.getAmountCents());
        PreviewPaymentResultDTO previewPaymentResult = barCodePaymentService.previewPayment(sanitizedTrxCode, previewPaymentRequestV2DTO.getAdditionalProperties(), amountCents);
        return mapPreviewPaymentV2(previewPaymentResult);
    }

    @Override
    @PerformanceLog(
            value = "BAR_CODE_RETRIEVE_PAYMENT",
            payloadBuilderBeanClass = TransactionBarCodeResponsePerfLoggerPayloadBuilder.class)
    public TransactionBarCodeResponse retrievePayment(String initiativeId, String userId) {
        return barCodePaymentService.findOldestNotAuthorized(userId, initiativeId);
    }

    @Override
    @PerformanceLog(
            value = "BAR_CODE_CAPTURE_PAYMENT",
            payloadBuilderBeanClass = TransactionBarCodeResponsePerfLoggerPayloadBuilder.class)
    public TransactionBarCodeResponse capturePayment(String trxCode) {
        return barCodePaymentService.capturePayment(trxCode);
    }

    @PerformanceLog(
            value = "BAR_CODE_CREATE_EXTENDED_TRANSACTION",
            payloadBuilderBeanClass = TransactionBarCodeResponsePerfLoggerPayloadBuilder.class)
    @Override
    public TransactionBarCodeResponse createExtendedTransaction(TransactionBarCodeCreationRequest trxBarCodeCreationRequest, String userId) {
        log.info("[BAR_CODE_CREATE_EXTENDED_TRANSACTION] The user {} is creating a transaction", Utilities.sanitizeString(userId));
        return barCodePaymentService.createExtendedTransaction(trxBarCodeCreationRequest, userId);
    }

    @PerformanceLog(
            value = "BAR_CODE_PDF")
    @Override
    public ResponseEntity<ReportDTO> downloadBarcode(
            String initiativeId,
            String trxCode,
            String userId,
            String username,
            String fiscalCode) {

        ReportDTO reportDTO = pdfService.create(initiativeId, trxCode, userId, username, fiscalCode);

        ContentDisposition cd = ContentDisposition
                .inline()
                .filename("barcode_" + trxCode + ".pdf", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.noStore())
                .body(reportDTO);
    }

    @PerformanceLog(
            value = "BAR_CODE_PREVIEW_PDF")
    @Override
    public ResponseEntity<ReportDTOWithTrxCode> downloadPreviewBarcode(
            String transactionId) {

        ReportDTOWithTrxCode reportDTO = pdfService.createPreauthPdf(transactionId);

        ContentDisposition cd = ContentDisposition
                .inline()
                .filename(reportDTO.getTrxCode() + "_preautorizzazione.pdf", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.noStore())
                .body(reportDTO);
    }

    private long validatePreviewAmount(BigDecimal amountCents) {
        final long previewAmountCents = amountCents.longValue();
        if (previewAmountCents < 0L) {
            log.info("[PREVIEW_TRANSACTION] Cannot preview transaction with negative amountCents: {}", previewAmountCents);
            throw new TransactionInvalidException(PaymentConstants.ExceptionCode.REWARD_NOT_VALID,
                    "Cannot preview transaction with negative amountCents [%s]".formatted(previewAmountCents));
        }
        return previewAmountCents;
    }

    private Map<String, String> buildLegacyPreviewAdditionalProperties(String productName, String productGtin) {
        Map<String, String> additionalProperties = new HashMap<>();
        if (productName != null) {
            additionalProperties.put(PRODUCT_NAME_KEY, productName);
        }
        if (productGtin != null) {
            additionalProperties.put(PRODUCT_GTIN_KEY, productGtin);
        }
        return additionalProperties;
    }

    private PreviewPaymentDTO mapPreviewPaymentV1(PreviewPaymentResultDTO previewPaymentResult, String productName, String productGtin) {
        return PreviewPaymentDTO.builder()
                .trxCode(previewPaymentResult.getTrxCode())
                .trxDate(previewPaymentResult.getTrxDate())
                .status(previewPaymentResult.getStatus())
                .originalAmountCents(previewPaymentResult.getOriginalAmountCents())
                .rewardCents(previewPaymentResult.getRewardCents())
                .residualAmountCents(previewPaymentResult.getResidualAmountCents())
                .userId(previewPaymentResult.getUserId())
                .productName(productName)
                .productGtin(productGtin)
                .extendedAuthorization(previewPaymentResult.isExtendedAuthorization())
                .build();
    }

    private PreviewPaymentResponseV2DTO mapPreviewPaymentV2(PreviewPaymentResultDTO previewPaymentResult) {
        return PreviewPaymentResponseV2DTO.builder()
                .trxCode(previewPaymentResult.getTrxCode())
                .trxDate(previewPaymentResult.getTrxDate())
                .status(previewPaymentResult.getStatus())
                .originalAmountCents(previewPaymentResult.getOriginalAmountCents())
                .rewardCents(previewPaymentResult.getRewardCents())
                .residualAmountCents(previewPaymentResult.getResidualAmountCents())
                .userId(previewPaymentResult.getUserId())
                .additionalProperties(previewPaymentResult.getAdditionalProperties())
                .extendedAuthorization(previewPaymentResult.isExtendedAuthorization())
                .build();
    }
}
