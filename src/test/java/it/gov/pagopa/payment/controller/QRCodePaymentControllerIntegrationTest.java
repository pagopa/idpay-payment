package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class QRCodePaymentControllerIntegrationTest extends BasePaymentControllerIntegrationTest {

    @Override
    protected String getChannel() {
        return RewardConstants.TRX_CHANNEL_QRCODE;
    }

    @Override
    protected MvcResult createTrx(TransactionCreationRequest trxRequest, String merchantId, String acquirerId, String idTrxAcquirer) throws Exception {
        return mockMvc
                .perform(
                        post("/idpay/payment/qr-code/merchant")
                                .header("x-merchant-id", merchantId)
                                .header("x-acquirer-id", acquirerId)
                                .header("x-apim-request-id", idTrxAcquirer)
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
    protected MvcResult confirmPayment(TransactionResponse trx, String merchantId, String acquirerId) throws Exception {
        return mockMvc
                .perform(
                        put("/idpay/payment/qr-code/merchant/{transactionId}/confirm", trx.getId())
                                .header("x-merchant-id", merchantId)
                                .header("x-acquirer-id", acquirerId))
                .andReturn();
    }

    @Override
    protected MvcResult cancelTrx(TransactionResponse trx, String merchantId, String acquirerId) throws Exception {
        return mockMvc
                .perform(
                        delete("/idpay/payment/qr-code/merchant/{transactionId}", trx.getId())
                                .header("x-merchant-id", merchantId)
                                .header("x-acquirer-id", acquirerId))
                .andReturn();
    }

    @Override
    protected MvcResult getStatusTransaction(String transactionId, String merchantId, String acquirerId) throws Exception {
        return mockMvc
                .perform(
                        get("/idpay/payment/qr-code/merchant/status/{transactionId}",transactionId)
                                .header("x-merchant-id",merchantId)
                                .header("x-acquirer-id",acquirerId)
                ).andReturn();
    }
}
