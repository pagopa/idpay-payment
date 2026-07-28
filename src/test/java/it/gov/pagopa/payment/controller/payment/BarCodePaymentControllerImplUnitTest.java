package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentRequestDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentResponseDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentResultDTO;
import it.gov.pagopa.payment.dto.ReportDTO;
import it.gov.pagopa.payment.dto.ReportDTOWithTrxCode;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.service.payment.BarCodePaymentService;
import it.gov.pagopa.payment.service.pdf.PdfService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

    @InjectMocks
    private BarCodePaymentControllerImpl controller;

    @Test
    void authPayment_shouldDelegateToService() {
        String initiativeId = "INITIATIVE_ID";
        String trxCode = "TRX_CODE";
        String merchantId = "MERCHANT_ID";
        String pointOfSaleId = "POS_ID";
        String acquirerId = "ACQUIRER_ID";
        AuthBarCodePaymentDTO authBarCodePaymentDTO = AuthBarCodePaymentDTO.builder()
                .amountCents(1000L)
                .idTrxAcquirer("ACQUIRER_TRX_ID")
                .build();

        AuthPaymentDTO expectedResponse = AuthPaymentDTO.builder()
                .id("TRX_ID")
                .status(SyncTrxStatus.AUTHORIZED)
                .build();

        when(barCodePaymentService.authPayment(initiativeId, trxCode, authBarCodePaymentDTO, merchantId, pointOfSaleId, acquirerId))
                .thenReturn(expectedResponse);

        AuthPaymentDTO result = controller.authPayment(initiativeId, trxCode, authBarCodePaymentDTO, merchantId, pointOfSaleId, acquirerId);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedResponse.getId(), result.getId());
        Assertions.assertEquals(SyncTrxStatus.AUTHORIZED, result.getStatus());

        verify(barCodePaymentService).authPayment(initiativeId, trxCode, authBarCodePaymentDTO, merchantId, pointOfSaleId, acquirerId);
    }


    @Test
    void previewPayment_shouldMapResponse() {
        PreviewPaymentRequestDTO request = PreviewPaymentRequestDTO.builder()
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

        when(barCodePaymentService.previewPayment("initiativeId", "trxCode", Map.of("customField", "customValue"), 100L))
                .thenReturn(previewPaymentResultDTO);

        PreviewPaymentResponseDTO result = controller.previewPayment("initiativeId", "trxCode", request);

        Assertions.assertEquals("trxCode", result.getTrxCode());
        Assertions.assertEquals(Map.of("customField", "validatedValue"), result.getAdditionalProperties());
        Assertions.assertTrue(result.isExtendedAuthorization());
        verify(barCodePaymentService).previewPayment("initiativeId", "trxCode", Map.of("customField", "customValue"), 100L);
    }

    @Test
    void downloadBarcode_shouldBuildInlineJsonResponse() {
        String initiativeId = "INITIATIVE_ID";
        String trxCode = "TRXCODE";
        String userId = "USER_ID";
        String username = "USERNAME";
        String fiscalCode = "FISCALCODE";

        ReportDTO report = ReportDTO.builder()
                .data("base64-content")
                .build();

        when(pdfService.create(initiativeId, trxCode, userId, username, fiscalCode)).thenReturn(report);

        ResponseEntity<ReportDTO> result = controller.downloadBarcode(initiativeId, trxCode, userId, username, fiscalCode);

        Assertions.assertEquals(200, result.getStatusCode().value());
        Assertions.assertEquals(MediaType.APPLICATION_JSON, result.getHeaders().getContentType());
        Assertions.assertEquals("no-store", result.getHeaders().getCacheControl());
        Assertions.assertTrue(result.getHeaders().getFirst("Content-Disposition").contains("barcode_TRXCODE.pdf"));
        Assertions.assertSame(report, result.getBody());

        verify(pdfService).create(initiativeId, trxCode, userId, username, fiscalCode);
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
