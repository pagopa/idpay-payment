package it.gov.pagopa.payment.controller.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.ValidationExceptionHandler;
import it.gov.pagopa.payment.configuration.PaymentErrorManagerConfig;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.service.payment.BarCodePaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        String expectedCode = "INVALID_REQUEST";
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
        assertEquals(expectedCode, actual.getCode());
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

        String actual = "{\"code\":\"INVALID_REQUEST\",\"message\":\"Required request header "
                + "'x-user-id' for method parameter type String is not present\"}";
        assertEquals(actual, result.getResponse().getContentAsString());
    }

    @Test
    void authorizeTransaction_testMandatoryFields() throws Exception {
        String expectedCode = "INVALID_REQUEST";
        List<String> expectedInvalidFields = List.of("amountCents");

        MvcResult result = mockMvc.perform(
                        put("/idpay/payment/bar-code/trxCode/authorize")
                                .header("x-merchant-id", "MERCHANT_ID")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorDTO actual = objectMapper.readValue(result.getResponse().getContentAsString(),
                ErrorDTO.class);
        assertEquals(expectedCode, actual.getCode());
        expectedInvalidFields.forEach(field -> assertTrue(actual.getMessage().contains(field)));
    }

    @Test
    void authorizeTransaction_testMandatoryHeaders() throws Exception {
        AuthBarCodePaymentDTO authBarCodePaymentDTO = AuthBarCodePaymentDTO.builder().amountCents(1000L).build();

        MvcResult result = mockMvc.perform(
                        put("/idpay/payment/bar-code/trxCode/authorize")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(authBarCodePaymentDTO)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertNotNull(result.getResponse().getContentAsString());

        String actual = "{\"code\":\"INVALID_REQUEST\",\"message\":\"Required request header "
                + "'x-merchant-id' for method parameter type String is not present\"}";
        assertEquals(actual, result.getResponse().getContentAsString());
    }
}
