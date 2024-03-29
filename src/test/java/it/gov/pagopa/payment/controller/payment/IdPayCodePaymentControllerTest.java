package it.gov.pagopa.payment.controller.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.common.web.exception.ValidationExceptionHandler;
import it.gov.pagopa.payment.configuration.PaymentErrorManagerConfig;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.dto.mapper.idpaycode.RelateUserResponseMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.service.payment.IdpayCodePaymentService;
import it.gov.pagopa.payment.test.fakers.TransactionCreationRequestFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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

@WebMvcTest(IdPayCodePaymentControllerImpl.class)
@Import({JsonConfig.class, ValidationExceptionHandler.class, PaymentErrorManagerConfig.class})
class IdPayCodePaymentControllerTest {

  @MockBean
  private IdpayCodePaymentService idpayCodePaymentServiceMock;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;
  @Test
  void relateUser_testMandatoryHeaders() throws Exception {

    TransactionCreationRequest body = TransactionCreationRequestFaker.mockInstance(1);

    MvcResult result = mockMvc.perform(
                    put("/idpay/payment/idpay-code/{transactionId}/relate-user",
                            "INITIATIVE_ID")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
            .andExpect(status().isBadRequest())
            .andReturn();

    assertNotNull(result.getResponse().getContentAsString());

    String actual = "{\"code\":\"PAYMENT_INVALID_REQUEST\",\"message\":\"Required request header "
            + "'Fiscal-Code' for method parameter type String is not present\"}";
    assertEquals(actual, result.getResponse().getContentAsString());
  }

  @Test
  void relateUser() throws Exception {

    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    RelateUserResponseMapper mapper = new RelateUserResponseMapper();

    RelateUserResponse resultUserResponse = mapper.transactionMapper(trx);

    Mockito.when(idpayCodePaymentServiceMock.relateUser(trx.getId(),trx.getMerchantFiscalCode())).thenReturn(resultUserResponse);
    MvcResult result = mockMvc.perform(
                    put("/idpay/payment/idpay-code/{transactionId}/relate-user",
                            trx.getId())
                            .header("Fiscal-Code",trx.getMerchantFiscalCode())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
            .andExpect(status().isOk())
            .andReturn();
    RelateUserResponse resultResponse = objectMapper.readValue(result.getResponse().getContentAsString(),RelateUserResponse.class);


    assertNotNull(result);
    assertEquals(resultResponse,resultUserResponse);


  }

}
