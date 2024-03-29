package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.common.web.exception.ValidationExceptionHandler;
import it.gov.pagopa.payment.configuration.PaymentErrorManagerConfig;
import it.gov.pagopa.payment.service.payment.QRCodePaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QRCodePaymentControllerImpl.class)
@Import({JsonConfig.class, PaymentErrorManagerConfig.class, ValidationExceptionHandler.class})
class QRCodePaymentControllerTest {

  public static final String EXPECTED_PAYMENT_GENERIC_ERROR =  "{\"code\":\"PAYMENT_GENERIC_ERROR\",\"message\":\"A generic error occurred\"}";
  @MockBean
  private QRCodePaymentService qrCodePaymentServiceMock;

  @Autowired
  private MockMvc mockMvc;

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
            EXPECTED_PAYMENT_GENERIC_ERROR,
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
            EXPECTED_PAYMENT_GENERIC_ERROR,
        result.getResponse().getContentAsString()
    );
  }


  @Test
  void unrelateUser() throws Exception {
    doNothing().when(qrCodePaymentServiceMock).unrelateUser(any(),any());
    MvcResult result = mockMvc.perform(
                    delete("/idpay/payment/qr-code/{trxCode}","TRXCODE")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("x-user-id", "USER_ID"))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

    assertEquals(HttpStatus.OK, HttpStatus.resolve(result.getResponse().getStatus()));


  }
}
