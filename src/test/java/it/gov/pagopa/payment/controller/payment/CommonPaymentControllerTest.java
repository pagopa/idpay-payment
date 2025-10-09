package it.gov.pagopa.payment.controller.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.ValidationExceptionHandler;
import it.gov.pagopa.payment.configuration.PaymentErrorManagerConfig;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.service.payment.common.*;
import it.gov.pagopa.payment.service.payment.expired.QRCodeExpirationService;
import it.gov.pagopa.payment.test.fakers.SyncTrxStatusFaker;
import it.gov.pagopa.payment.test.fakers.TransactionCreationRequestFaker;
import it.gov.pagopa.payment.test.fakers.TransactionResponseFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommonPaymentControllerImpl.class)
@Import({JsonConfig.class, ValidationExceptionHandler.class, PaymentErrorManagerConfig.class})
class CommonPaymentControllerTest {

  @MockitoBean
  @Qualifier("commonCreate")
  private CommonCreationServiceImpl commonCreationServiceMock;
  @MockitoBean
  @Qualifier("commonConfirm")
  private CommonConfirmServiceImpl commonConfirmServiceMock;
  @MockitoBean
  @Qualifier("commonReversal")
  private CommonReversalServiceImpl commonReversalService;
  @MockitoBean
  @Qualifier("commonReward")
  private CommonRewardServiceImpl commonRewardService;

  @MockitoBean
  @Qualifier("commonCancel")
  private CommonCancelServiceImpl commonCancelServiceMock;

  @MockitoBean
  private QRCodeExpirationService qrCodeExpirationServiceMock;

  @MockitoBean
  private CommonStatusTransactionServiceImpl commonStatusTransactionServiceMock;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private static final String ACQUIRER_ID = "ACQUIRERID1";
  private static final String ID_TRX_ISSUER = "IDTRXISSUER1";
  private static final String MERCHANT_ID = "MERCHANTID1";
  private static final String POINT_OF_SALE_ID = "POINT_OF_SALE_ID1";
  private static final String TRANSACTION_ID = "TRANSACTIONID1";
  private static final String INITIATIVE_ID = "INITIATIVEID1";

  @Test
  void createCommonTransaction_testMandatoryFields() throws Exception {
    List<String> expectedInvalidFields = Arrays.asList("initiativeId", "amountCents",
        "idTrxAcquirer");

    MvcResult result = mockMvc.perform(
            post("/idpay/payment/")
                .header("x-merchant-id", MERCHANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andReturn();

    ErrorDTO actual = objectMapper.readValue(result.getResponse().getContentAsString(),
        ErrorDTO.class);

    Assertions.assertEquals(ExceptionCode.PAYMENT_INVALID_REQUEST, actual.getCode());
    expectedInvalidFields.forEach(
        field -> Assertions.assertTrue(actual.getMessage().contains(field)));
  }

  @Test
  void createCommonTransaction() throws Exception {
    TransactionCreationRequest body = TransactionCreationRequestFaker.mockInstance(1);
    TransactionResponse response = TransactionResponseFaker.mockInstance(1);
    Mockito.when(commonCreationServiceMock.createTransaction(body, null, MERCHANT_ID, ACQUIRER_ID,
        ID_TRX_ISSUER)).thenReturn(response);

    MvcResult result = mockMvc.perform(MockMvcRequestBuilders
        .post("/idpay/payment/")
        .content(objectMapper.writeValueAsString(body))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-merchant-id", MERCHANT_ID)
        .header("x-acquirer-id", ACQUIRER_ID)
        .header("x-apim-request-id", ID_TRX_ISSUER)
    ).andExpect(status().isCreated()).andReturn();

    TransactionResponse resultResponse = objectMapper.readValue(
        result.getResponse().getContentAsString(),
        TransactionResponse.class);

    Assertions.assertNotNull(resultResponse);
    Assertions.assertEquals(response, resultResponse);
    Mockito.verify(commonCreationServiceMock)
        .createTransaction(body, null, MERCHANT_ID, ACQUIRER_ID, ID_TRX_ISSUER);
  }

  @Test
  void confirmCommonTransactionTestMandatoryHeaders() throws Exception {

    MvcResult result = mockMvc.perform(MockMvcRequestBuilders
            .put("/idpay/payment/{transactionId}/confirm", TRANSACTION_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
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
  void confirmCommonTransaction() throws Exception {
    TransactionResponse response = TransactionResponseFaker.mockInstance(1);

    Mockito.when(commonConfirmServiceMock.confirmPayment(TRANSACTION_ID, MERCHANT_ID, ACQUIRER_ID))
        .thenReturn(response);

    MvcResult result = mockMvc.perform(MockMvcRequestBuilders
        .put("/idpay/payment/{transactionId}/confirm", TRANSACTION_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .header("x-merchant-id", MERCHANT_ID)
        .header("x-acquirer-id", ACQUIRER_ID)
    ).andExpect(status().is2xxSuccessful()).andReturn();

    TransactionResponse resultResponse = objectMapper.readValue(
        result.getResponse().getContentAsString(),
        TransactionResponse.class);

    Assertions.assertNotNull(resultResponse);
    Assertions.assertEquals(response, resultResponse);
    Mockito.verify(commonConfirmServiceMock)
        .confirmPayment(TRANSACTION_ID, MERCHANT_ID, ACQUIRER_ID);
  }

  @Test
  void cancelTransaction() throws Exception {

    MvcResult result = mockMvc.perform(MockMvcRequestBuilders
            .delete("/idpay/payment/transactions/{transactionId}",
                TRANSACTION_ID)
            .header("x-merchant-id", MERCHANT_ID)
            .header("x-acquirer-id", ACQUIRER_ID)
            .header("x-point-of-sale-id", POINT_OF_SALE_ID))
        .andExpect(status().isOk())
        .andReturn();

    assertEquals("", result.getResponse().getContentAsString());
    Mockito.verify(commonCancelServiceMock)
        .cancelTransaction(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
            Mockito.anyString());

  }

  @Test
  void cancelTransaction_testMandatoryHeaders() throws Exception {

    MvcResult result = mockMvc.perform(MockMvcRequestBuilders
            .delete("/idpay/payment/transactions/{transactionId}",
                TRANSACTION_ID)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andReturn();

    assertNotNull(result.getResponse().getContentAsString());

    String actual = "{\"code\":\"PAYMENT_INVALID_REQUEST\",\"message\":\"Required request header "
        + "'x-merchant-id' for method parameter type String is not present\"}";
    assertEquals(actual, result.getResponse().getContentAsString());
  }

  @Test
  void reversalTransaction() throws Exception {
    MultipartFile file = Mockito.mock(MultipartFile.class);

    MvcResult result = mockMvc.perform(MockMvcRequestBuilders
            .multipart("/idpay/payment/transactions/{transactionId}/reversal", TRANSACTION_ID)
            .file("file", file.getBytes())
            .header("x-merchant-id", MERCHANT_ID)
            .header("x-point-of-sale-id", POINT_OF_SALE_ID)
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isNoContent())
        .andReturn();

    assertEquals("", result.getResponse().getContentAsString());
    Mockito.verify(commonReversalService)
        .reversalTransaction(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
            Mockito.any());
  }

  @Test
  void rewardTransaction() throws Exception {
    MultipartFile file = Mockito.mock(MultipartFile.class);

    MvcResult result = mockMvc.perform(MockMvcRequestBuilders
            .multipart("/idpay/payment/transactions/{transactionId}/reward", TRANSACTION_ID)
            .file("file", file.getBytes())
            .header("x-merchant-id", MERCHANT_ID)
            .header("x-point-of-sale-id", POINT_OF_SALE_ID)
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isNoContent())
        .andReturn();

    assertEquals("", result.getResponse().getContentAsString());
    Mockito.verify(commonRewardService)
        .rewardTransaction(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
            Mockito.any());
  }

  @Test
  void getStatusTransaction_testMandatoryHeaders() throws Exception {

    MvcResult result = mockMvc.perform(MockMvcRequestBuilders
            .get("/idpay/payment/{transactionId}/status",
                TRANSACTION_ID)
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
  void getStatusTransaction() throws Exception {
    SyncTrxStatusDTO response = SyncTrxStatusFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);

    Mockito.when(
            commonStatusTransactionServiceMock.getStatusTransaction(TRANSACTION_ID, MERCHANT_ID))
        .thenReturn(response);

    MvcResult result = mockMvc.perform(MockMvcRequestBuilders
            .get("/idpay/payment/{transactionId}/status",
                TRANSACTION_ID)
            .header("x-merchant-id", MERCHANT_ID)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().is2xxSuccessful())
        .andReturn();

    SyncTrxStatusDTO resultResponse = objectMapper.readValue(
        result.getResponse().getContentAsString(),
        SyncTrxStatusDTO.class);

    Assertions.assertNotNull(resultResponse);
    Assertions.assertEquals(response, resultResponse);
    Mockito.verify(commonStatusTransactionServiceMock)
        .getStatusTransaction(TRANSACTION_ID, MERCHANT_ID);

  }

  @Test
  void forceConfirmTrxExpiration() throws Exception {

    Mockito.when(qrCodeExpirationServiceMock.forceConfirmTrxExpiration(INITIATIVE_ID))
        .thenReturn(1L);

    MvcResult result = mockMvc.perform(MockMvcRequestBuilders
            .put("/idpay/payment/force-expiration/confirm/{initiativeId}",
                INITIATIVE_ID)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().is2xxSuccessful())
        .andReturn();

    Assertions.assertNotNull(result);
    Assertions.assertEquals(1L, Long.valueOf(result.getResponse().getContentAsString()));
    Mockito.verify(qrCodeExpirationServiceMock).forceConfirmTrxExpiration(INITIATIVE_ID);
  }

  @Test
  void forceAuthorizationTrxExpiration() throws Exception {

    Mockito.when(qrCodeExpirationServiceMock.forceAuthorizationTrxExpiration(INITIATIVE_ID))
        .thenReturn(1L);

    MvcResult result = mockMvc.perform(MockMvcRequestBuilders
            .put("/idpay/payment/force-expiration/authorization/{initiativeId}",
                INITIATIVE_ID)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().is2xxSuccessful())
        .andReturn();

    Assertions.assertNotNull(result);
    Assertions.assertEquals(1L, Long.valueOf(result.getResponse().getContentAsString()));
    Mockito.verify(qrCodeExpirationServiceMock).forceAuthorizationTrxExpiration(INITIATIVE_ID);
  }

  @Test
  void cancelPendingTransactions_ok() throws Exception {
    Mockito.doNothing().when(commonCancelServiceMock).rejectPendingTransactions();

    mockMvc.perform(MockMvcRequestBuilders
            .delete("/idpay/payment/pendingTransactions")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    Mockito.verify(commonCancelServiceMock, Mockito.times(1)).rejectPendingTransactions();
    Mockito.verifyNoMoreInteractions(commonCancelServiceMock);
  }
}
