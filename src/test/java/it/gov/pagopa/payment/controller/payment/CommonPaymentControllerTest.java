package it.gov.pagopa.payment.controller.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.ValidationExceptionHandler;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.service.payment.common.CommonCancelServiceImpl;
import it.gov.pagopa.payment.service.payment.common.CommonCreationServiceImpl;
import it.gov.pagopa.payment.test.fakers.TransactionCreationRequestFaker;
import it.gov.pagopa.payment.test.fakers.TransactionResponseFaker;
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

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommonPaymentControllerImpl.class)
@Import({JsonConfig.class, ValidationExceptionHandler.class})
class CommonPaymentControllerTest {
    @MockBean
    @Qualifier("CommonCreate")
    private CommonCreationServiceImpl commonCreationServiceMock;

    @MockBean
    @Qualifier("CommonCancel")
    private CommonCancelServiceImpl commonCancelServiceMock;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ACQUIRER_ID = "ACQUIRERID1";
    private static final String ID_TRX_ISSUER = "IDTRXISSUER1";
    private static final String MERCHANT_ID = "MERCHANTID1";
    private static final String TRX_ID = "TRXID";

    @Test
    void createCommonTransaction_testMandatoryFields() throws Exception {
        String expectedCode = "INVALID_REQUEST";
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

        Assertions.assertEquals(expectedCode, actual.getCode());
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
    void cancelTransaction() throws Exception {

        MvcResult result = mockMvc.perform(
                        delete("/idpay/payment/{transactionId}",
                                TRX_ID)
                                .header("x-merchant-id",MERCHANT_ID)
                                .header("x-acquirer-id" ,ACQUIRER_ID))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals("", result.getResponse().getContentAsString());
        Mockito.verify(commonCancelServiceMock).cancelTransaction(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

    }
    @Test
    void cancelTransaction_testMandatoryHeaders() throws Exception {

        MvcResult result = mockMvc.perform(
                        delete("/idpay/payment/{transactionId}",
                                TRX_ID)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertNotNull(result.getResponse().getContentAsString());

        String actual = "{\"code\":\"INVALID_REQUEST\",\"message\":\"Required request header "
                + "'x-merchant-id' for method parameter type String is not present\"}";
        assertEquals(actual, result.getResponse().getContentAsString());
    }

}