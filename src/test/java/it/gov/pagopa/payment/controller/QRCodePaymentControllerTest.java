package it.gov.pagopa.payment.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.payment.configuration.JsonConfig;
import it.gov.pagopa.payment.exception.ValidationExceptionHandler;
import it.gov.pagopa.payment.service.QRCodePaymentService;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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

    MvcResult result = mockMvc.perform(
            post("/idpay/payment/qr-code/merchant")
                .header("x-merchant-id", "MERCHANT_ID")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andReturn();

    assertNotNull(result.getResponse().getContentAsString());
  }

  @Test
  void createTransaction_testMandatoryHeaders() throws Exception {

    Map<String, Object> body = Map.of(
        "initiativeId", "initiativeId",
        "trxDate", OffsetDateTime.now(),
        "amountCents", 123);

    MvcResult result = mockMvc.perform(
            post("/idpay/payment/qr-code/merchant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isBadRequest())
        .andReturn();

    assertNotNull(result.getResponse().getContentAsString());
  }

}
