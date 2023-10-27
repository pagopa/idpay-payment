package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionCreationRequestFaker;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.apache.commons.lang3.function.FailableConsumer;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@TestPropertySource(
        properties = {
                "logging.level.it.gov.pagopa.payment.service.payment.QRCodePaymentServiceImpl=WARN"
        })
class QRCodePaymentControllerIntegrationTest extends BasePaymentControllerIntegrationTest {

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
    protected void checkCreateChannel(String storedChannel) {
        assertEquals(getChannel(), storedChannel);
    }

    @Override
    protected <T> T extractResponseAuthCannotRelateUser(TransactionResponse trxCreated, String userId) throws Exception {
       return extractResponse(authTrx(trxCreated, userId, MERCHANTID), HttpStatus.FORBIDDEN, null);
    }

    @Override
    protected List<FailableConsumer<Integer, Exception>> getExtraUseCases() {
        List<FailableConsumer<Integer, Exception>> qrCodeUseCases = new ArrayList<>();

        {
            //useCase 20: user cancel payment instead of authorizing
            qrCodeUseCases.add(i -> {
                TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
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
            });

            // useCase 21: Another User auth transaction
            qrCodeUseCases.add(i -> {
                TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
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
            });
        }

        return qrCodeUseCases;
    }
}
