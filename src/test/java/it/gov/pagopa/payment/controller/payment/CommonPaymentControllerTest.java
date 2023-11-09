package it.gov.pagopa.payment.controller.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.ValidationExceptionHandler;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.service.payment.common.CommonCancelServiceImpl;
import it.gov.pagopa.payment.service.payment.common.CommonConfirmServiceImpl;
import it.gov.pagopa.payment.service.payment.common.CommonCreationServiceImpl;
import it.gov.pagopa.payment.service.payment.common.CommonStatusTransactionServiceImpl;
import it.gov.pagopa.payment.test.fakers.SyncTrxStatusFaker;
import it.gov.pagopa.payment.test.fakers.TransactionCreationRequestFaker;
import it.gov.pagopa.payment.test.fakers.TransactionResponseFaker;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(CommonPaymentControllerImpl.class)
@Import({JsonConfig.class, ValidationExceptionHandler.class})
class CommonPaymentControllerTest {
    @MockBean
    @Qualifier("CommonCreate")
    private CommonCreationServiceImpl commonCreationServiceMock;
    @MockBean
    @Qualifier("CommonConfirm")
    private CommonConfirmServiceImpl commonConfirmServiceMock;

    @MockBean
    @Qualifier("CommonCancel")
    private CommonCancelServiceImpl commonCancelServiceMock;

    @MockBean
    private CommonStatusTransactionServiceImpl commonStatusTransactionServiceMock;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ACQUIRER_ID = "ACQUIRERID1";
    private static final String ID_TRX_ISSUER = "IDTRXISSUER1";
    private static final String MERCHANT_ID = "MERCHANTID1";
    private static final String TRANSACTION_ID = "TRANSACTIONID1";

    @Test
    void createCommonTransaction_testMandatoryFields() throws Exception {
        List<String> expectedInvalidFields = Arrays.asList("initiativeId", "amountCents", "idTrxAcquirer");

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
        expectedInvalidFields.forEach(field -> Assertions.assertTrue(actual.getMessage().contains(field)));
    }

    @Test
    void createCommonTransaction() throws Exception {
        TransactionCreationRequest body = TransactionCreationRequestFaker.mockInstance(1);
        TransactionResponse response = TransactionResponseFaker.mockInstance(1);
        Mockito.when(commonCreationServiceMock.createTransaction(body,null,MERCHANT_ID,ACQUIRER_ID,ID_TRX_ISSUER)).thenReturn(response);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/idpay/payment/")
                        .content(objectMapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("x-merchant-id",MERCHANT_ID)
                        .header("x-acquirer-id" ,ACQUIRER_ID)
                        .header("x-apim-request-id", ID_TRX_ISSUER)
                ).andExpect(status().isCreated()).andReturn();


        TransactionResponse resultResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TransactionResponse.class);

        Assertions.assertNotNull(resultResponse);
        Assertions.assertEquals(response,resultResponse);
        Mockito.verify(commonCreationServiceMock).createTransaction(body,null,MERCHANT_ID,ACQUIRER_ID,ID_TRX_ISSUER);
    }

    @Test
    void confirmCommonTransactionTestMandatoryHeaders() throws Exception{

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

        Mockito.when(commonConfirmServiceMock.confirmPayment(TRANSACTION_ID,MERCHANT_ID,ACQUIRER_ID)).thenReturn(response);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .put("/idpay/payment/{transactionId}/confirm", TRANSACTION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-merchant-id",MERCHANT_ID)
                        .header("x-acquirer-id" ,ACQUIRER_ID)
                ).andExpect(status().is2xxSuccessful()).andReturn();

        TransactionResponse resultResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TransactionResponse.class);

        Assertions.assertNotNull(resultResponse);
        Assertions.assertEquals(response,resultResponse);
        Mockito.verify(commonConfirmServiceMock).confirmPayment(TRANSACTION_ID,MERCHANT_ID,ACQUIRER_ID);
    }

    @Test
    void cancelTransaction() throws Exception {

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .delete("/idpay/payment/{transactionId}",
                                TRANSACTION_ID)
                                .header("x-merchant-id",MERCHANT_ID)
                                .header("x-acquirer-id" ,ACQUIRER_ID))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals("", result.getResponse().getContentAsString());
        Mockito.verify(commonCancelServiceMock).cancelTransaction(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

    }
    @Test
    void cancelTransaction_testMandatoryHeaders() throws Exception {

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .delete("/idpay/payment/{transactionId}",
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
    void getStatusTransaction() throws Exception{
        SyncTrxStatusDTO response = SyncTrxStatusFaker.mockInstance(1,SyncTrxStatus.AUTHORIZED );

        Mockito.when(commonStatusTransactionServiceMock.getStatusTransaction(TRANSACTION_ID)).thenReturn(response);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get("/idpay/payment/{transactionId}/status",
                                TRANSACTION_ID)
                        .header("x-merchant-id",MERCHANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        SyncTrxStatusDTO resultResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                SyncTrxStatusDTO.class);

        Assertions.assertNotNull(resultResponse);
        Assertions.assertEquals(response,resultResponse);
        Mockito.verify(commonStatusTransactionServiceMock).getStatusTransaction(TRANSACTION_ID);

    }
}