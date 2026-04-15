package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.PreviewPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentRequestDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentRequestV2DTO;
import it.gov.pagopa.payment.dto.PreviewPaymentResponseV2DTO;
import it.gov.pagopa.payment.dto.PreviewPaymentResultDTO;
import it.gov.pagopa.payment.dto.ReportDTOWithTrxCode;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.service.payment.BarCodePaymentService;
import it.gov.pagopa.payment.service.pdf.PdfService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarCodePaymentControllerImplUnitTest {

    @Mock
    private BarCodePaymentService barCodePaymentService;
    @Mock
    private PdfService pdfService;

    private BarCodePaymentControllerImpl controller;

    @BeforeEach
    void setUp() {
        controller = new BarCodePaymentControllerImpl(barCodePaymentService, pdfService);
    }

    @Test
    void previewPayment_withoutProductGtin_shouldSkipLegacyProperty() {
        PreviewPaymentRequestDTO request = PreviewPaymentRequestDTO.builder()
                .productName("product")
                .productGtin(null)
                .amountCents(BigDecimal.valueOf(100))
                .build();
        PreviewPaymentResultDTO previewPaymentResultDTO = PreviewPaymentResultDTO.builder()
                .trxCode("trxCode")
                .trxDate(OffsetDateTime.now())
                .status(SyncTrxStatus.AUTHORIZED)
                .originalAmountCents(700L)
                .rewardCents(100L)
                .residualAmountCents(600L)
                .userId("userId")
                .additionalProperties(Map.of("productName", "validatedProduct"))
                .extendedAuthorization(false)
                .build();

        when(barCodePaymentService.previewPayment("trxCode", Map.of("productName", "product"), 100L))
                .thenReturn(previewPaymentResultDTO);

        PreviewPaymentDTO result = controller.previewPayment("trxCode", request);

        Assertions.assertEquals("product", result.getProductName());
        Assertions.assertNull(result.getProductGtin());
        verify(barCodePaymentService).previewPayment("trxCode", Map.of("productName", "product"), 100L);
    }

    @Test
    void previewPaymentV2_shouldMapResponse() {
        PreviewPaymentRequestV2DTO request = PreviewPaymentRequestV2DTO.builder()
                .amountCents(BigDecimal.valueOf(100))
                .additionalProperties(Map.of("customField", "customValue"))
                .build();
        PreviewPaymentResultDTO previewPaymentResultDTO = PreviewPaymentResultDTO.builder()
                .trxCode("trxCode")
                .trxDate(OffsetDateTime.now())
                .status(SyncTrxStatus.AUTHORIZED)
                .originalAmountCents(700L)
                .rewardCents(100L)
                .residualAmountCents(600L)
                .userId("userId")
                .additionalProperties(Map.of("customField", "validatedValue"))
                .extendedAuthorization(true)
                .build();

        when(barCodePaymentService.previewPayment("trxCode", Map.of("customField", "customValue"), 100L))
                .thenReturn(previewPaymentResultDTO);

        PreviewPaymentResponseV2DTO result = controller.previewPaymentV2("trxCode", request);

        Assertions.assertEquals("trxCode", result.getTrxCode());
        Assertions.assertEquals(Map.of("customField", "validatedValue"), result.getAdditionalProperties());
        Assertions.assertTrue(result.isExtendedAuthorization());
        verify(barCodePaymentService).previewPayment("trxCode", Map.of("customField", "customValue"), 100L);
    }

    @Test
    void downloadPreviewBarcode_shouldBuildInlineJsonResponse() {
        ReportDTOWithTrxCode report = ReportDTOWithTrxCode.builder()
                .trxCode("TRXCODE")
                .data("base64-content")
                .build();
        when(pdfService.createPreauthPdf("transactionId")).thenReturn(report);

        ResponseEntity<ReportDTOWithTrxCode> result = controller.downloadPreviewBarcode("transactionId");

        Assertions.assertEquals(200, result.getStatusCode().value());
        Assertions.assertEquals(MediaType.APPLICATION_JSON, result.getHeaders().getContentType());
        Assertions.assertEquals("no-store", result.getHeaders().getCacheControl());
        Assertions.assertTrue(result.getHeaders().getFirst("Content-Disposition").contains("TRXCODE_preautorizzazione.pdf"));
        Assertions.assertSame(report, result.getBody());
        verify(pdfService).createPreauthPdf("transactionId");
    }
}
