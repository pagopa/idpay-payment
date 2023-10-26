package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.dto.mapper.AuthPaymentMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.TransactionCreationRequestFaker;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.apache.commons.lang3.function.FailableConsumer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

public class IdPayCodePaymentControllerIntegrationTest extends BasePaymentControllerIntegrationTest {

    @Autowired
    private AuthPaymentMapper authPaymentMapper;
    @Autowired
    private TransactionInProgressRepository transactionInProgressRepository;

    @Override
    protected String getChannel() {
        return RewardConstants.TRX_CHANNEL_IDPAYCODE;
    }

    @Override
    protected MvcResult createTrx(TransactionCreationRequest trxRequest, String merchantId, String acquirerId, String idTrxIssuer) throws Exception {
        return mockMvc
                .perform(
                        post("/idpay/payment/")
                                .header("x-merchant-id", merchantId)
                                .header("x-acquirer-id", acquirerId)
                                .header("x-apim-request-id", idTrxIssuer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(trxRequest)))
                .andReturn();
    }

    @Override
    protected MvcResult preAuthTrx(TransactionResponse trx, String userid, String merchantId) throws Exception {
        // relate-user
        MvcResult resultRelateUser = relateUserTrx(trx, userid);
        if (resultRelateUser.getResponse().getStatus() != HttpStatus.OK.value()){
            return resultRelateUser;
        }
        // preview
        return previewTrx(trx, merchantId);
    }

    @NotNull
    private MvcResult relateUserTrx(TransactionResponse trx, String userid) throws Exception {
        return mockMvc
                .perform(
                        put("/idpay/payment/idpay-code/{transactionId}/relate-user", trx.getId())
                                .header("Fiscal-Code", userid))
                .andReturn();
    }
    @NotNull
    private MvcResult previewTrx(TransactionResponse trx, String merchantId) throws Exception {
        return mockMvc
                .perform(
                        put("/idpay/mil/payment/idpay-code/{transactionId}/preview", trx.getId())
                                .header("x-merchant-id", merchantId))
                .andReturn();
    }



    @Override
    protected MvcResult authTrx(TransactionResponse trx, String userid, String merchantId) throws Exception {
        PinBlockDTO pinBlockOk = PinBlockDTO.builder() //TODO add stub
                .pinBlock("PINBLOCK")
                .encryptedKey("ENCRYPTEDKEY")
                .build();
        return authTrx(trx, merchantId, pinBlockOk);
    }

    @NotNull
    private MvcResult authTrx(TransactionResponse trx, String merchantId, PinBlockDTO pinBlockBody) throws Exception {
        return mockMvc
                .perform(
                        put("/idpay/mil/payment/idpay-code/{transactionId}/authorize", trx.getId())
                                .header("x-merchant-id", merchantId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(pinBlockBody)))
                .andReturn();
    }

    //TODO with generics methods
    @Override
    protected MvcResult unrelateTrx(TransactionResponse trx, String userId) throws Exception {//TODO fix duplicate code
        return mockMvc
                .perform(
                        delete("/idpay/payment/qr-code/{trxCode}", trx.getTrxCode())
                                .header("x-user-id", userId))
                .andReturn();
    }

    @Override
    protected MvcResult confirmPayment(TransactionResponse trx, String merchantId, String acquirerId) throws Exception {//TODO fix duplicate code
        return mockMvc
                .perform(
                        put("/idpay/payment/qr-code/merchant/{transactionId}/confirm", trx.getId())
                                .header("x-merchant-id", merchantId)
                                .header("x-acquirer-id", acquirerId))
                .andReturn();
    }

    @Override
    protected MvcResult cancelTrx(TransactionResponse trx, String merchantId, String acquirerId) throws Exception {//TODO fix duplicate code
        return mockMvc
                .perform(
                        delete("/idpay/payment/qr-code/merchant/{transactionId}", trx.getId())
                                .header("x-merchant-id", merchantId)
                                .header("x-acquirer-id", acquirerId))
                .andReturn();
    }

    @Override
    protected MvcResult getStatusTransaction(String transactionId, String merchantId) throws Exception {//TODO fix duplicate code
        return mockMvc
                .perform(
                        get("/idpay/payment/{transactionId}/status",transactionId)
                                .header("x-merchant-id",merchantId)
                ).andReturn();
    }

    @Override
    protected MvcResult forceAuthExpiration(String initiativeId) throws Exception { //TODO fix duplicate code
        return mockMvc
                .perform(
                        put("/idpay/payment/qr-code/force-expiration/authorization/{initiativeId}",initiativeId)
                ).andReturn();
    }

    @Override
    protected MvcResult forceConfirmExpiration(String initiativeId) throws Exception { //TODO fix duplicate code
        return mockMvc
                .perform(
                        put("/idpay/payment/qr-code/force-expiration/confirm/{initiativeId}",initiativeId)
                ).andReturn();
    }

    @Override
    protected void checkCreateChannel(String storedChannel) {
        assertNull(storedChannel);
    }

    @Override
    protected <T> T extractResponseAuthCannotRelateUser(TransactionResponse trxCreated, String userId) throws Exception {
        return extractResponse(authTrx(trxCreated, userId, MERCHANTID), HttpStatus.BAD_REQUEST, null); //TODO BAD request into idpay-code
    }

    @Override
    protected List<FailableConsumer<Integer, Exception>> getExtraUseCases() {
        List<FailableConsumer<Integer, Exception>> idPayCodeUseCases = new ArrayList<>();

        {
            // useCase 21: Preview from another merchant
            idPayCodeUseCases.add(i -> {
                TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
                trxRequest.setInitiativeId(INITIATIVEID);

                // Creating transaction
                TransactionResponse trxCreated = createTrxSuccess(trxRequest);
                assertTrxCreatedData(trxRequest, trxCreated);

                // Relate User
                extractResponse(relateUserTrx(trxCreated, USERID), HttpStatus.OK, RelateUserResponse.class);

                // Preview
                extractResponse(previewTrx(trxCreated, "DUMMYMERCHANTID"), HttpStatus.FORBIDDEN, AuthPaymentDTO.class);

            });

            // useCase 22: Preview before relate transaction
            idPayCodeUseCases.add(i -> {
                TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
                trxRequest.setInitiativeId(INITIATIVEID);

                // Creating transaction
                TransactionResponse trxCreated = createTrxSuccess(trxRequest);
                assertTrxCreatedData(trxRequest, trxCreated);

                // Preview
                AuthPaymentDTO authResponse = extractResponse(previewTrx(trxCreated, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);

                //Check response
                TransactionInProgress trxStored = transactionInProgressRepository.findById(trxCreated.getId()).orElse(null);
                Assertions.assertNotNull(trxStored);
                trxStored.setRewards(null); //TODO check
                Assertions.assertEquals(authPaymentMapper.transactionMapper(trxStored),authResponse);
            });

            // useCase 23: Error retrieve second factor
            idPayCodeUseCases.add(i -> {
                String userWithoutSecondFactor = "NOTSECONDFACTOR";
                TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
                trxRequest.setInitiativeId(INITIATIVEID);

                // Creating transaction
                TransactionResponse trxCreated = createTrxSuccess(trxRequest);
                assertTrxCreatedData(trxRequest, trxCreated);

                // Relate User
                extractResponse(relateUserTrx(trxCreated, userWithoutSecondFactor), HttpStatus.OK, RelateUserResponse.class);

                // Preview
                extractResponse(previewTrx(trxCreated, MERCHANTID), HttpStatus.NOT_FOUND, AuthPaymentDTO.class);
            });

            // useCase 24: Error retrieve pinBlock
            idPayCodeUseCases.add(i -> {
                String userWithoutSecondFactor = "NOTPINBLOCKUSERID";
                TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
                trxRequest.setInitiativeId(INITIATIVEID);

                // Creating transaction
                TransactionResponse trxCreated = createTrxSuccess(trxRequest);
                assertTrxCreatedData(trxRequest, trxCreated);

                // Relate User
                extractResponse(relateUserTrx(trxCreated, userWithoutSecondFactor), HttpStatus.OK, RelateUserResponse.class);

                // Preview
                extractResponse(previewTrx(trxCreated, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
                //
                extractResponse(authTrx(trxCreated, userWithoutSecondFactor, MERCHANTID), HttpStatus.FORBIDDEN,null);
            });

        }

        return idPayCodeUseCases;
    }
}
