package it.gov.pagopa.payment.controller.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.ValidationExceptionHandler;
import it.gov.pagopa.payment.configuration.PaymentErrorManagerConfig;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.service.payment.QRCodePaymentService;
import it.gov.pagopa.payment.service.payment.expired.QRCodeExpirationService;
import it.gov.pagopa.payment.test.fakers.TransactionCreationRequestFaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QRCodePaymentControllerImpl.class)
@Import({JsonConfig.class, PaymentErrorManagerConfig.class, ValidationExceptionHandler.class})
class QRCodePaymentControllerTest {

  @MockBean
  private QRCodePaymentService qrCodePaymentServiceMock;
  @MockBean
  private QRCodeExpirationService qrCodeExpirationServiceMock;

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

  @Test
  void preAuthTransaction_testGenericError() throws Exception {
    when(qrCodePaymentServiceMock.relateUser(any(), any())).thenThrow(RuntimeException.class);
    MvcResult result = mockMvc.perform(
            put("/idpay/payment/qr-code/trxCode/relate-user")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-user-id", "USER_ID"))
        .andExpect(status().isInternalServerError())
        .andReturn();

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.resolve(result.getResponse().getStatus()));
    assertEquals(
        "{\"code\":\"PAYMENT_GENERIC_ERROR\",\"message\":\"A generic error occurred for payment\"}",
        result.getResponse().getContentAsString()
    );
  }

  @Test
  void authorizeTransaction_testGenericError() throws Exception {
    when(qrCodePaymentServiceMock.authPayment(any(), any())).thenThrow(RuntimeException.class);
    MvcResult result = mockMvc.perform(
            put("/idpay/payment/qr-code/trxCode/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-user-id", "USER_ID"))
        .andExpect(status().isInternalServerError())
        .andReturn();

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.resolve(result.getResponse().getStatus()));
    assertEquals(
        "{\"code\":\"PAYMENT_GENERIC_ERROR\",\"message\":\"A generic error occurred for payment\"}",
        result.getResponse().getContentAsString()
    );
  }
}
