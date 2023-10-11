package it.gov.pagopa.payment.controller.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.payment.BaseIntegrationTest;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.test.fakers.TransactionCreationRequestFaker;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class BarCodePaymentControllerIntegrationTest extends BaseIntegrationTest {

    protected String getChannel() {
        return RewardConstants.TRX_CHANNEL_BARCODE;
    }
    private final String USERID = "USERID";
    private final String MERCHANTID = "MERCHANTID";


    protected MvcResult createTrx(TransactionBarCodeCreationRequest trxRequest, String userId) throws Exception {
        return mockMvc
                .perform(
                        post("/idpay/payment/bar-code")
                                .header("x-user-id", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(trxRequest)))
                .andReturn();
    }


    protected MvcResult authTrx(String trxCode, AuthBarCodePaymentDTO paymentDTO, String merchantId) throws Exception {
        return mockMvc
                .perform(
                        put("/idpay/payment/bar-code/{trxCode}/authorize", trxCode)
                                .header("x-merchant-id", merchantId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(paymentDTO)))
                .andReturn();
    }

    @Test
    void createTransaction_InitiativeNotExist() throws Exception {
        // useCase 0: initiative not existent
        TransactionBarCodeCreationRequest trxRequest = TransactionBarCodeCreationRequest.builder()
                .initiativeId("DUMMYINITIATIVEID").build();
        extractResponse(createTrx(trxRequest, USERID), HttpStatus.NOT_FOUND, null);

        // Other APIs cannot be invoked because we have not a valid trxId
        TransactionBarCodeResponse dummyTrx = TransactionBarCodeResponse.builder().id("DUMMYTRXID").trxCode("dummytrxcode").trxDate(OffsetDateTime.now()).build();
        extractResponse(authTrx(dummyTrx, USERID, MERCHANTID), HttpStatus.NOT_FOUND, null);
    }

    protected <T> T extractResponse(MvcResult response, HttpStatus expectedHttpStatusCode, Class<T> expectedBodyClass) {
        assertEquals(expectedHttpStatusCode.value(), response.getResponse().getStatus());
        if (expectedBodyClass != null) {
            try {
                return objectMapper.readValue(response.getResponse().getContentAsString(), expectedBodyClass);
            } catch (JsonProcessingException | UnsupportedEncodingException e) {
                throw new IllegalStateException("Cannot read body response!", e);
            }
        } else {
            return null;
        }
    }

}
