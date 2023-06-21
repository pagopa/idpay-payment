package it.gov.pagopa.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.common.web.exception.ValidationExceptionHandler;
import it.gov.pagopa.payment.service.QRCodePaymentService;
import it.gov.pagopa.payment.test.fakers.TransactionCreationRequestFaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QRCodePaymentControllerImpl.class)
@Import({JsonConfig.class, ValidationExceptionHandler.class})
class QRCodePaymentControllerTest {

  @MockBean
  private QRCodePaymentService qrCodePaymentService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void createTransaction_testMandatoryFields() throws Exception {
    String expectedCode = "INVALID_REQUEST";
    List<String> expectedInvalidFields = Arrays.asList("initiativeId", "amountCents", "idTrxAcquirer");

    MvcResult result = mockMvc.perform(
            post("/idpay/payment/qr-code/merchant")
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
  void createTransaction_testMandatoryHeaders() throws Exception {

    TransactionCreationRequest body = TransactionCreationRequestFaker.mockInstance(1);

    MvcResult result = mockMvc.perform(
            post("/idpay/payment/qr-code/merchant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isBadRequest())
        .andReturn();

    assertNotNull(result.getResponse().getContentAsString());

    String actual = "{\"code\":\"INVALID_REQUEST\",\"message\":\"Required request header "
        + "'x-merchant-id' for method parameter type String is not present\"}";
    assertEquals(actual, result.getResponse().getContentAsString());
  }
}
