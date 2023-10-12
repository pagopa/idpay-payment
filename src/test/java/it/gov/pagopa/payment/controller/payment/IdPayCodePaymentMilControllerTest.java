package it.gov.pagopa.payment.controller.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.ValidationExceptionHandler;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.service.payment.IdpayCodePaymentService;
import it.gov.pagopa.payment.test.fakers.TransactionCreationRequestFaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
