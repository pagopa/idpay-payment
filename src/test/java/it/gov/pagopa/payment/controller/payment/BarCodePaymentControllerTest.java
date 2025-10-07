package it.gov.pagopa.payment.controller.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.ValidationExceptionHandler;
import it.gov.pagopa.payment.configuration.PaymentErrorManagerConfig;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentRequestDTO;
import it.gov.pagopa.payment.dto.ReportDTO;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.service.payment.BarCodePaymentService;
import it.gov.pagopa.payment.service.pdf.PdfService;
import it.gov.pagopa.payment.test.fakers.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BarCodePaymentControllerImpl.class)
@Import({JsonConfig.class, PaymentErrorManagerConfig.class, ValidationExceptionHandler.class})
class BarCodePaymentControllerTest {

    @MockitoBean
    private BarCodePaymentService barCodePaymentService;

    @MockitoBean
    private PdfService pdfService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void captureCommonTransactionByTrxCode() throws Exception {
        TransactionBarCodeResponse txrResponse = TransactionBarCodeResponseFaker.mockInstance(1);

        Mockito.when(barCodePaymentService.capturePayment(any())).thenReturn(txrResponse);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                .put("/idpay/payment/bar-code/{trxCode}/capture","trxCode")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().is2xxSuccessful()).andReturn();

        TransactionBarCodeResponse resultResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TransactionBarCodeResponse.class);

        Assertions.assertNotNull(resultResponse);
        Assertions.assertEquals(txrResponse,resultResponse);
    }


    @Test
    void createTransaction() throws Exception {
        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequestFaker.mockInstance(1);


        TransactionBarCodeResponse txrResponse = TransactionBarCodeResponseFaker.mockInstance(1);
        when(barCodePaymentService.createTransaction(trxCreationReq,"USER_ID")).thenReturn(txrResponse);

        MvcResult result = mockMvc.perform(
                        post("/idpay/payment/bar-code")
                                .header("x-user-id", "USER_ID")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(trxCreationReq))
                            ).andExpect(status().isCreated()).andReturn();

        TransactionBarCodeResponse resultResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TransactionBarCodeResponse.class);

        Assertions.assertNotNull(resultResponse);
        Assertions.assertEquals(txrResponse,resultResponse);

    }
    @Test
    void createTransaction_testMandatoryFields() throws Exception {
        List<String> expectedInvalidFields = List.of("initiativeId");

        MvcResult result = mockMvc.perform(
                        post("/idpay/payment/bar-code")
                                .header("x-user-id", "USER_ID")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorDTO actual = objectMapper.readValue(result.getResponse().getContentAsString(),
                ErrorDTO.class);
        assertEquals(ExceptionCode.PAYMENT_INVALID_REQUEST, actual.getCode());
        expectedInvalidFields.forEach(field -> assertTrue(actual.getMessage().contains(field)));
    }

    @Test
    void createTransaction_testMandatoryHeaders() throws Exception {
        TransactionBarCodeResponse body = TransactionBarCodeResponse.builder().initiativeId("INITIATIVE_ID").build();

        MvcResult result = mockMvc.perform(
                        post("/idpay/payment/bar-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertNotNull(result.getResponse().getContentAsString());

        String actual = "{\"code\":\"PAYMENT_INVALID_REQUEST\",\"message\":\"Required request header "
                + "'x-user-id' for method parameter type String is not present\"}";
        assertEquals(actual, result.getResponse().getContentAsString());
    }

    @Test
    void authorizeTransaction() throws Exception {

        AuthBarCodePaymentDTO authBarCodePaymentDTO = AuthBarCodePaymentDTO.builder()
                .amountCents(1000L)
                .idTrxAcquirer("ACQUIRERID1")
                .build();

        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1,SyncTrxStatus.CREATED);
        AuthPaymentDTO authPaymentDTO =  AuthPaymentDTOFaker.mockInstance(1,trx);
        authPaymentDTO.setStatus(SyncTrxStatus.AUTHORIZED);
        authPaymentDTO.setRewards(null);

        when(barCodePaymentService.authPayment(trx.getTrxCode(),authBarCodePaymentDTO,"MERCHANTID1", "POINTOFSALEID1", "ACQUIRERID1")).thenReturn(authPaymentDTO);

        MvcResult result = mockMvc.perform(
                        put("/idpay/payment/bar-code/{trxCode}/authorize",trx.getTrxCode())
                                .header("x-merchant-id", "MERCHANTID1")
                                .header("x-point-of-sale-id", "POINTOFSALEID1")
                                .header("x-acquirer-id", "ACQUIRERID1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(authBarCodePaymentDTO)))
                .andExpect(status().isOk())
                .andReturn();

        AuthPaymentDTO resultResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthPaymentDTO.class);

        Assertions.assertNotNull(resultResponse);
        Assertions.assertEquals(authPaymentDTO,resultResponse);

    }

    @Test
    void authorizeTransaction_testMandatoryFields() throws Exception {
        List<String> expectedInvalidFields = List.of("amountCents");

        MvcResult result = mockMvc.perform(
                        put("/idpay/payment/bar-code/trxCode/authorize")
                                .header("x-merchant-id", "MERCHANT_ID")
                                .header("x-acquirer-id", "ACQUIRER_ID")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorDTO actual = objectMapper.readValue(result.getResponse().getContentAsString(),
                ErrorDTO.class);
        assertEquals(ExceptionCode.PAYMENT_INVALID_REQUEST, actual.getCode());
        expectedInvalidFields.forEach(field -> assertTrue(actual.getMessage().contains(field)));
    }

    @Test
    void authorizeTransaction_testMandatoryHeaders() throws Exception {
        AuthBarCodePaymentDTO authBarCodePaymentDTO = AuthBarCodePaymentDTO.builder()
                .amountCents(1000L)
                .idTrxAcquirer("ID_TRX_ACQUIRER")
                .build();

        MvcResult result = mockMvc.perform(
                        put("/idpay/payment/bar-code/trxCode/authorize")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(authBarCodePaymentDTO)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertNotNull(result.getResponse().getContentAsString());

        String actual = "{\"code\":\"PAYMENT_INVALID_REQUEST\",\"message\":\"Required request header "
                + "'x-merchant-id' for method parameter type String is not present\"}";
        assertEquals(actual, result.getResponse().getContentAsString());
    }

    @Test
    void previewPayment_ok() throws Exception {
        PreviewPaymentDTO previewPaymentDTO = PreviewPaymentDTOFaker.mockInstance();
        PreviewPaymentRequestDTO previewPaymentRequestDTO = PreviewPaymentRequestDTOFaker.mockInstance();

        when(barCodePaymentService.previewPayment(any(), any(), any())).thenReturn(previewPaymentDTO);
        MvcResult result = mockMvc.perform(
                        put("/idpay/payment/bar-code/{trxCode}/preview","trxCode")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(previewPaymentRequestDTO)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertNotNull(result.getResponse().getContentAsString());
    }

    @Test
    void previewPayment_negativeAmount() throws Exception {
        PreviewPaymentDTO previewPaymentDTO = PreviewPaymentDTOFaker.mockInstance();
        PreviewPaymentRequestDTO previewPaymentRequestDTO = PreviewPaymentRequestDTOFaker.mockInstance();
        previewPaymentRequestDTO.setAmountCents(BigDecimal.valueOf(-100L));

        when(barCodePaymentService.previewPayment(any(), any(), any())).thenReturn(previewPaymentDTO);
        MvcResult result = mockMvc.perform(
                        put("/idpay/payment/bar-code/{trxCode}/preview", "trxCode")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(previewPaymentRequestDTO)))
                .andExpect(status().is5xxServerError())
                .andReturn();

        assertNotNull(result.getResponse().getContentAsString());
    }

    @Test
    void retrievePayment_ok() throws Exception {
        String userId = "USER_ID";
        String initiativeId = "INITIATIVE_ID";
        TransactionBarCodeResponse txrResponse = TransactionBarCodeResponseFaker.mockInstance(1);
        when(barCodePaymentService.findOldestNotAuthorized(userId, initiativeId)).thenReturn(txrResponse);

        MvcResult result = mockMvc.perform(
                get("/idpay/payment/initiatives/{initiativeId}/bar-code", initiativeId)
                        .header("x-user-id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        TransactionBarCodeResponse resultResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TransactionBarCodeResponse.class);

        Assertions.assertNotNull(resultResponse);
        Assertions.assertEquals(txrResponse,resultResponse);

    }

    @Test
    void createExtendedransaction() throws Exception {
        TransactionBarCodeCreationRequest trxCreationReq = TransactionBarCodeCreationRequestFaker.mockInstance(1);


        TransactionBarCodeResponse txrResponse = TransactionBarCodeResponseFaker.mockInstance(1);
        when(barCodePaymentService.createExtendedTransaction(trxCreationReq,"USER_ID")).thenReturn(txrResponse);

        MvcResult result = mockMvc.perform(
                post("/idpay/payment/bar-code/extended")
                        .header("x-user-id", "USER_ID")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(trxCreationReq))
        ).andExpect(status().isCreated()).andReturn();

        TransactionBarCodeResponse resultResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TransactionBarCodeResponse.class);

        Assertions.assertNotNull(resultResponse);
        Assertions.assertEquals(txrResponse,resultResponse);

    }

    @Test
    void downloadBarcode_ok() throws Exception {
        // arrange
        String initiativeId = "INITIATIVE_ID";
        String trxCode = "TRX123";
        String userId = "USER_42";

        byte[] expectedPdfBytes = new byte[]{1, 2, 3, 4, 5};
        String base64 = Base64.getEncoder().encodeToString(expectedPdfBytes);
        ReportDTO report = ReportDTO.builder().data(base64).build();

        when(pdfService.create(initiativeId, trxCode, userId)).thenReturn(report);

        // act
        MvcResult result = mockMvc.perform(
                get("/idpay/payment/initiatives/{initiativeId}/bar-code/{trxCode}/pdf", initiativeId, trxCode)
                        .header("x-user-id", userId)
        ).andExpect(status().isOk()).andReturn();

        // assert: Content-Type application/json
        String contentType = result.getResponse().getContentType();
        assertNotNull(contentType, "Content-Type mancante");
        assertTrue(contentType.startsWith(MediaType.APPLICATION_JSON_VALUE),
                "Content-Type atteso application/json ma trovato: " + contentType);

        // assert: Content-Disposition inline; filename="barcode_TRX123.pdf"
        String cd = result.getResponse().getHeader("Content-Disposition");
        assertNotNull(cd, "Content-Disposition mancante");
        String cdLower = cd.toLowerCase();
        assertTrue(cdLower.contains("inline"), "Content-Disposition non contiene 'inline': " + cd);
        assertTrue(cd.contains("barcode_" + trxCode + ".pdf"),
                "Content-Disposition non contiene il filename atteso: " + cd);

        // assert: Cache-Control no-store
        assertEquals("no-store", result.getResponse().getHeader("Cache-Control"));

        // assert: body JSON con ReportDTO, campo data == base64
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode json = objectMapper.readTree(body);
        assertTrue(json.hasNonNull("data"), "Campo 'data' assente nel body");
        assertEquals(base64, json.get("data").asText(), "Il campo 'data' non corrisponde alla Base64 attesa");

        // verify
        verify(pdfService).create(initiativeId, trxCode, userId);
        verifyNoMoreInteractions(pdfService);
    }


    @Test
    void downloadBarcode_errorFromService_returns5xx() throws Exception {
        String initiativeId = "INITIATIVE_ID";
        String trxCode = "TRX123";
        String userId = "USER_42";

        when(pdfService.create(initiativeId, trxCode, userId))
                .thenThrow(new IllegalStateException("pdf generation failed"));

        mockMvc.perform(
                get("/idpay/payment/initiatives/{initiativeId}/bar-code/{trxCode}/pdf", initiativeId, trxCode)
                        .header("x-user-id", userId)
                        .accept(MediaType.TEXT_PLAIN) // <-- NON application/pdf
        ).andExpect(status().is5xxServerError());
    }

    @Test
    void downloadBarcode_missingUserIdHeader_returns400() throws Exception {
        String initiativeId = "INITIATIVE_ID";
        String trxCode = "TRX123";

        mockMvc.perform(
                get("/idpay/payment/initiatives/{initiativeId}/bar-code/{trxCode}/pdf", initiativeId, trxCode)
                        .accept(MediaType.TEXT_PLAIN) // <-- NON application/pdf
        ).andExpect(status().isBadRequest());
    }
}
