package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class QRCodePaymentControllerIntegrationTest extends BasePaymentControllerIntegrationTest {

    @Override
    protected MvcResult createTrx(TransactionCreationRequest trxRequest, String merchantId) throws Exception {
        return mockMvc
                .perform(
                        post("/idpay/payment/qr-code/merchant")
                                .header("x-merchant-id", merchantId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(trxRequest)))
                .andReturn();
    }

    @Override
    protected MvcResult preAuthTrx(TransactionResponse trx, String userid, String merchantId) throws Exception {
        return mockMvc
                .perform(
                        put("/idpay/payment/qr-code/{trxCode}/relate-user", trx.getTrxCode())
                                .header("x-user-id", userid))
                .andReturn();
    }

    @Override
    protected MvcResult authTrx(TransactionResponse trx, String userid, String merchantId) throws Exception {
        return mockMvc
                .perform(
                        put("/idpay/payment/qr-code/{trxCode}/authorize", trx.getTrxCode())
                                .header("x-user-id", userid))
                .andReturn();
    }

    @Override
    protected MvcResult confirmPayment(TransactionResponse trx, String merchantId) throws Exception {
        return mockMvc
                .perform(
                        put("/idpay/payment/qr-code/merchant/{transactionId}/confirm", trx.getId())
                                .header("x-merchant-id", merchantId))
                .andReturn();
    }
}
