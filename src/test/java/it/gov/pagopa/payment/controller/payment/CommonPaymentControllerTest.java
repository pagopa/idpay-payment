package it.gov.pagopa.payment.controller.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.ValidationExceptionHandler;
import it.gov.pagopa.payment.dto.common.BaseTransactionResponseDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.service.payment.common.CommonCreationServiceImpl;
import it.gov.pagopa.payment.test.fakers.BaseTransactionResponseFaker;
import it.gov.pagopa.payment.test.fakers.TransactionCreationRequestFaker;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommonPaymentControllerImpl.class)
@Import({JsonConfig.class, ValidationExceptionHandler.class})
class CommonPaymentControllerTest {
    @MockBean
    @Qualifier("CommonCreate")
    private CommonCreationServiceImpl commonCreationServiceMock;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ACQUIRER_ID = "ACQUIRERID1";
    private static final String ID_TRX_ISSUER = "IDTRXISSUER1";
    private static final String MERCHANT_ID = "MERCHANTID1";

    @Test
    void createCommonTransaction_testMandatoryFields() throws Exception {
        String expectedCode = "INVALID_REQUEST";
        List<String> expectedInvalidFields = Arrays.asList("initiativeId", "amountCents", "idTrxAcquirer");

        MvcResult result = mockMvc.perform(
                        post("/idpay/mil/payment/")
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
    void createCommonTransaction() throws Exception {
        TransactionCreationRequest body = TransactionCreationRequestFaker.mockInstance(1);
        BaseTransactionResponseDTO response = BaseTransactionResponseFaker.mockInstance(1);
        Mockito.when(commonCreationServiceMock.createTransaction(body,null,MERCHANT_ID,ACQUIRER_ID,ID_TRX_ISSUER)).thenReturn(response);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/idpay/mil/payment/")
                        .content(objectMapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("x-merchant-id",MERCHANT_ID)
                        .header("x-acquirer-id" ,ACQUIRER_ID)
                        .header("x-apim-request-id", ID_TRX_ISSUER)
                ).andExpect(status().isCreated()).andReturn();


        BaseTransactionResponseDTO resultResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                BaseTransactionResponseDTO.class);

        Assertions.assertNotNull(resultResponse);
        Assertions.assertEquals(response,resultResponse);
        Mockito.verify(commonCreationServiceMock).createTransaction(body,null,MERCHANT_ID,ACQUIRER_ID,ID_TRX_ISSUER);
    }

}