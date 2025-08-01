package it.gov.pagopa.payment.controller.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.ValidationExceptionHandler;
import it.gov.pagopa.payment.configuration.PaymentErrorManagerConfig;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.service.payment.IdpayCodePaymentService;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.PinBlockDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IdPayCodePaymentMilControllerImpl.class)
@Import({JsonConfig.class, ValidationExceptionHandler.class, PaymentErrorManagerConfig.class})
class IdPayCodePaymentMilControllerTest {

  @MockitoBean
  private IdpayCodePaymentService idpayCodePaymentServiceMock;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;
  private static final String MERCHANT_ID = "MERCHANTID1";
  private static final Object TRANSACTION_ID = "TRANSACTIONID1";

  @Test
  void previewPayment() throws Exception {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    AuthPaymentDTO authPaymentDTO =  AuthPaymentDTOFaker.mockInstance(1,trx);
    authPaymentDTO.setRewards(null);

    Mockito.when(idpayCodePaymentServiceMock.previewPayment(trx.getId(),trx.getMerchantId())).thenReturn(authPaymentDTO);


    MvcResult result = mockMvc.perform(
                    put("/idpay/mil/payment/idpay-code/{transactionId}/preview",
                            trx.getId())
                            .header("x-merchant-id",trx.getMerchantId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
            .andExpect(status().isOk())
            .andReturn();

    AuthPaymentDTO resultPaymentDTO = objectMapper.readValue(result.getResponse().getContentAsString(),AuthPaymentDTO.class);

    assertNotNull(resultPaymentDTO);
    assertEquals(authPaymentDTO,resultPaymentDTO);

  }
  @Test
  void previewPayment_testMandatoryHeaders() throws Exception {

    MvcResult result = mockMvc.perform(
                    put("/idpay/mil/payment/idpay-code/{transactionId}/preview",
                            "INITIATIVE_ID")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andReturn();

    ErrorDTO actual = objectMapper.readValue(result.getResponse().getContentAsString(),
            ErrorDTO.class);
    assertEquals(ExceptionCode.PAYMENT_INVALID_REQUEST, actual.getCode());
    assertEquals("Required request header "
                    + "'x-merchant-id' for method parameter type String is not present",
            actual.getMessage());

  }

  @Test
  void authorization() throws Exception {
    PinBlockDTO pinBlockDTO = PinBlockDTOFaker.mockInstance();
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    AuthPaymentDTO authPaymentDTO =  AuthPaymentDTOFaker.mockInstance(1,trx);
    authPaymentDTO.setRewards(null);

    Mockito.when(idpayCodePaymentServiceMock.authPayment(trx.getId(),trx.getMerchantId(),pinBlockDTO)).thenReturn(authPaymentDTO);

    MvcResult result = mockMvc.perform(
                    put("/idpay/mil/payment/idpay-code/{transactionId}/authorize",trx.getId())
                            .header("x-merchant-id", trx.getMerchantId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(pinBlockDTO)))
            .andExpect(status().isOk())
            .andReturn();

    AuthPaymentDTO resultPaymentDTO = objectMapper.readValue(result.getResponse().getContentAsString(),AuthPaymentDTO.class);

    assertNotNull(resultPaymentDTO);
    assertEquals(authPaymentDTO,resultPaymentDTO);
  }

  @Test
  void auth_testMandatoryFields() throws Exception {
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

    assertEquals(ExceptionCode.PAYMENT_INVALID_REQUEST, actual.getCode());
    expectedInvalidFields.forEach(field -> assertTrue(actual.getMessage().contains(field)));
  }
  @Test
  void authPayment_testMandatoryHeaders() throws Exception {

    MvcResult result = mockMvc.perform(
                    put("/idpay/mil/payment/idpay-code/{transactionId}/authorize",TRANSACTION_ID)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andReturn();

    ErrorDTO actual = objectMapper.readValue(result.getResponse().getContentAsString(),
            ErrorDTO.class);
    assertEquals(ExceptionCode.PAYMENT_INVALID_REQUEST, actual.getCode());
    assertEquals("Required request header "
                    + "'x-merchant-id' for method parameter type String is not present",
            actual.getMessage());

  }
}
