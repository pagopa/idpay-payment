package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionCreationRequestFaker;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class QRCodePaymentControllerIntegrationTestDeprecated extends BasePaymentControllerIntegrationTestDeprecated {

    @Override
    protected String getChannel() {
        return RewardConstants.TRX_CHANNEL_QRCODE;
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

    /**
     * Unrelate user API acting as <i>userId</i>
     */
    protected MvcResult unrelateTrx(TransactionResponse trx, String userId) throws Exception {
        return mockMvc
                .perform(
                        delete("/idpay/payment/qr-code/{trxCode}", trx.getTrxCode())
                                .header("x-user-id", userId))
                .andReturn();
    }

    @Override
    protected <T> T extractResponseAuthCannotRelateUser(TransactionResponse trxCreated, String userId) throws Exception {
       return extractResponse(authTrx(trxCreated, userId, MERCHANTID), HttpStatus.FORBIDDEN, null);
    }

    @Test
    @SneakyThrows
    void test_userCancelPaymentInsteadOfAuthorizing() {
        TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(bias);
        trxRequest.setInitiativeId(INITIATIVEID);

        // Creating transaction
        TransactionResponse trxCreated = createTrxSuccess(trxRequest);
        TransactionInProgress trxInProgressCreated = checkIfStored(trxCreated.getId());

        // Relating to user
        AuthPaymentDTO preAuthResult = extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
        assertEquals(SyncTrxStatus.IDENTIFIED, preAuthResult.getStatus());
        checkTransactionStored(preAuthResult, USERID);

        extractResponse(unrelateTrx(trxCreated, USERID + "1"), HttpStatus.FORBIDDEN, null);
        extractResponse(unrelateTrx(trxCreated, USERID), HttpStatus.OK, null);

        TransactionInProgress unrelated = checkIfStored(trxCreated.getId());
        cleanDatesAndCheckUnrelatedTrx(trxInProgressCreated, unrelated);
    }

    @Test
    @SneakyThrows
    void test_anotherUserAuthTransaction() {
        TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(bias);
        trxRequest.setInitiativeId(INITIATIVEID);

        // Creating transaction
        TransactionResponse trxCreated = createTrxSuccess(trxRequest);
        assertTrxCreatedData(trxRequest, trxCreated);

        // Cannot invoke other APIs if not relating first
        extractResponse(authTrx(trxCreated, USERID, MERCHANTID), HttpStatus.BAD_REQUEST, null);
        extractResponse(confirmPayment(trxCreated, MERCHANTID, ACQUIRERID), HttpStatus.BAD_REQUEST, null);
        updateStoredTransaction(trxCreated.getId(), t -> {
            // resetting throttling data in order to assert preAuth data
            t.setTrxChargeDate(null);
            t.setElaborationDateTime(null);
        });

        // Relating to user
        AuthPaymentDTO preAuthResult = extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
        assertEquals(SyncTrxStatus.IDENTIFIED, preAuthResult.getStatus());
        assertPreAuthData(preAuthResult, true);


        // Cannot invoke other APIs if not authorizing first
        extractResponse(confirmPayment(trxCreated, MERCHANTID, ACQUIRERID), HttpStatus.BAD_REQUEST, null);
        updateStoredTransaction(trxCreated.getId(), t -> {
            // resetting throttling data in order to assert auth data
            t.setElaborationDateTime(null);
        });

        // Only the right userId could authorize its transaction
        extractResponse(authTrx(trxCreated, "DUMMYUSERID", MERCHANTID), HttpStatus.FORBIDDEN, null);
    }
}
