package it.gov.pagopa.payment.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.payment.configuration.JsonConfig;
import it.gov.pagopa.payment.exception.CustomExceptionHandler;
import it.gov.pagopa.payment.exception.ErrorManager;
import it.gov.pagopa.payment.service.QRCodePaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(QRCodePaymentControllerImpl.class)
@Import({JsonConfig.class, CustomExceptionHandler.class, ErrorManager.class})
class QRCodePaymentControllerTest {
  @MockBean
  private QRCodePaymentService qrCodePaymentService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void createTransaction_testMandatoryFields() throws Exception {
    MvcResult result = mockMvc.perform(
            post("/idpay/payment/qr-code/merchant")
                .header("x-merchant-id", "MECHANT_ID")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andReturn();
    assertEquals("{}", result.getResponse().getContentAsString());
  }

}
