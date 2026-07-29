package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.*;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.service.payment.BarCodePaymentService;
import it.gov.pagopa.payment.service.pdf.PdfService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarCodePaymentControllerImplUnitTest {

    @Mock
    private BarCodePaymentService barCodePaymentService;
    @Mock
    private PdfService pdfService;

    @InjectMocks
    private BarCodePaymentControllerImpl controller;

    @Test
    void previewPayment_shouldMapResponse() {
        String initiativeId = "initiativeId";
        String trxCode = "trxCode";
        PreviewPaymentRequestDTO request = PreviewPaymentRequestDTO.builder()
                .amountCents(BigDecimal.valueOf(100))
                .additionalProperties(Map.of("customField", "customValue"))
                .build();
        PreviewPaymentResultDTO previewPaymentResultDTO = PreviewPaymentResultDTO.builder()
                .trxCode("trxCode")
                .trxDate(OffsetDateTime.now(ZoneId.of("Europe/Rome")))
                .status(SyncTrxStatus.AUTHORIZED)
                .originalAmountCents(100L)
                .rewardCents(100L)
                .residualAmountCents(0L)
                .userId("userId")
                .additionalProperties(Map.of("customField", "validatedValue"))
                .extendedAuthorization(false)
                .build();

        when(barCodePaymentService.previewPayment(initiativeId, trxCode, Map.of("customField", "customValue"), 100L))
                .thenReturn(previewPaymentResultDTO);

        PreviewPaymentResponseDTO result = controller.previewPayment(initiativeId, trxCode, request);

        Assertions.assertEquals("trxCode", result.getTrxCode());
        Assertions.assertEquals(Map.of("customField", "validatedValue"), result.getAdditionalProperties());
        verify(barCodePaymentService).previewPayment(initiativeId, trxCode, Map.of("customField", "customValue"), 100L);
    }

    @Test
    void previewPayment_shouldMapExtendedAuthorization() {
        String initiativeId = "initiativeId";
        String trxCode = "trxCode";
        PreviewPaymentRequestDTO request = PreviewPaymentRequestDTO.builder()
                .amountCents(BigDecimal.valueOf(100))
                .additionalProperties(Map.of("customField", "customValue"))
                .build();
        PreviewPaymentResultDTO previewPaymentResultDTO = PreviewPaymentResultDTO.builder()
                .trxCode("trxCode")
                .trxDate(OffsetDateTime.now(ZoneId.of("Europe/Rome")))
                .status(SyncTrxStatus.AUTHORIZED)
                .originalAmountCents(700L)
                .rewardCents(100L)
                .residualAmountCents(600L)
                .userId("userId")
                .additionalProperties(Map.of("customField", "validatedValue"))
                .extendedAuthorization(true)
                .build();

        when(barCodePaymentService.previewPayment(initiativeId, trxCode, Map.of("customField", "customValue"), 100L))
                .thenReturn(previewPaymentResultDTO);

        PreviewPaymentResponseDTO result = controller.previewPayment(initiativeId, trxCode, request);

        Assertions.assertEquals("trxCode", result.getTrxCode());
        Assertions.assertEquals(Map.of("customField", "validatedValue"), result.getAdditionalProperties());
        Assertions.assertTrue(result.isExtendedAuthorization());
        verify(barCodePaymentService).previewPayment(initiativeId, trxCode, Map.of("customField", "customValue"), 100L);
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
        String contentDisposition = result.getHeaders().getFirst("Content-Disposition");
        Assertions.assertNotNull(contentDisposition);
        Assertions.assertTrue(contentDisposition.contains("TRXCODE_preautorizzazione.pdf"));
        Assertions.assertSame(report, result.getBody());
        verify(pdfService).createPreauthPdf("transactionId");
    }
}
