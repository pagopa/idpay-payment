package it.gov.pagopa.payment.controller.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.ValidationExceptionHandler;
import it.gov.pagopa.payment.service.payment.IdpayCodePaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IdPayCodePaymentMilControllerImpl.class)
@Import({JsonConfig.class, ValidationExceptionHandler.class})
class IdPayCodePaymentMilControllerTest {

  @MockBean
  private IdpayCodePaymentService idpayCodePaymentServiceMock;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;
  private static final String MERCHANT_ID = "MERCHANTID1";
  private static final Object TRANSACTION_ID = "TRANSACTIONID1";
  @Test
  void previewPayment_testMandatoryHeaders() throws Exception {
    String expectedCode = "INVALID_REQUEST";

    MvcResult result = mockMvc.perform(
                    put("/idpay/mil/payment/idpay-code/{transactionId}/preview",
                            "INITIATIVE_ID")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andReturn();

    ErrorDTO actual = objectMapper.readValue(result.getResponse().getContentAsString(),
            ErrorDTO.class);
    assertEquals(expectedCode, actual.getCode());
    assertEquals("Required request header "
                    + "'x-merchant-id' for method parameter type String is not present",
            actual.getMessage());

  }

  @Test
  void auth_testMandatoryFields() throws Exception {
    String expectedCode = "INVALID_REQUEST";
    List<String> expectedInvalidFields = List.of("pinBlock","encryptedKey");

    MvcResult result = mockMvc.perform(
                    put("/idpay/mil/payment/idpay-code/{transactionId}/authorize",TRANSACTION_ID)
                            .header("x-merchant-id", MERCHANT_ID)
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
  void authPayment_testMandatoryHeaders() throws Exception {
    String expectedCode = "INVALID_REQUEST";

    MvcResult result = mockMvc.perform(
                    put("/idpay/mil/payment/idpay-code/{transactionId}/authorize",TRANSACTION_ID)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andReturn();

    ErrorDTO actual = objectMapper.readValue(result.getResponse().getContentAsString(),
            ErrorDTO.class);
    assertEquals(expectedCode, actual.getCode());
    assertEquals("Required request header "
                    + "'x-merchant-id' for method parameter type String is not present",
            actual.getMessage());

  }
}