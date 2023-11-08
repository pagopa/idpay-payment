package it.gov.pagopa.payment.controller.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.ValidationExceptionHandler;
import it.gov.pagopa.payment.configuration.PaymentErrorManagerConfig;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.service.payment.BarCodePaymentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(BarCodePaymentControllerImpl.class)
@Import({JsonConfig.class, PaymentErrorManagerConfig.class, ValidationExceptionHandler.class})
class BarCodePaymentControllerTest {

    @MockBean
    private BarCodePaymentService barCodePaymentService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void authorizeTransaction_testMandatoryFields() throws Exception {
        List<String> expectedInvalidFields = List.of("amountCents", "idTrxAcquirer");

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
}
