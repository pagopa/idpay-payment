package it.gov.pagopa.payment.controller.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.payment.BaseIntegrationTest;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.InitiativeRewardType;
import it.gov.pagopa.payment.model.InitiativeConfig;
import it.gov.pagopa.payment.model.RewardRule;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class BarCodePaymentControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RewardRuleRepository rewardRuleRepository;
    @Autowired
    private TransactionInProgressRepository transactionInProgressRepository;

    private final String USERID = "USERID";
    private final String MERCHANTID = "MERCHANTID";
    public static final String INITIATIVEID = "INITIATIVEID";
    public static final String BARCODE_INITIATIVEID_REWARDED = "BARCODE_INITIATIVEID_REWARDED";
    public static final String BARCODE_INITIATIVEID_NO_BUDGET = "BARCODE_INITIATIVEID_NO_BUDGET";
    public static final String INITIATIVEID_NOT_STARTED = INITIATIVEID + "1";
    public static final LocalDate TODAY = LocalDate.now();


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

    /*
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
     */

    @Test
    void authorizeTransaction_budgetExhausted() throws Exception{
        TransactionBarCodeCreationRequest trxRequest = TransactionBarCodeCreationRequest.builder()
                .initiativeId(BARCODE_INITIATIVEID_NO_BUDGET)
                .build();
        AuthBarCodePaymentDTO authBarCodePaymentDTO = AuthBarCodePaymentDTO.builder().amountCents(10000L).build();
        rewardRuleRepository.save(RewardRule.builder().id(BARCODE_INITIATIVEID_NO_BUDGET)
                .initiativeConfig(InitiativeConfig.builder()
                        .initiativeId(BARCODE_INITIATIVEID_NO_BUDGET)
                        .initiativeRewardType(InitiativeRewardType.DISCOUNT)
                        .startDate(TODAY.minusDays(1))
                        .endDate(TODAY.plusDays(1))
                        .build())
                .build());

        // Creating transaction
        TransactionResponse trxCreated = extractResponse(createTrx(trxRequest, USERID), HttpStatus.CREATED, TransactionResponse.class);

        // Cannot invoke other APIs if REJECTED
        extractResponse(authTrx(trxCreated.getTrxCode(), authBarCodePaymentDTO, MERCHANTID), HttpStatus.FORBIDDEN, null);
    }

    @Test
    void authorizeTransaction() throws Exception{
        rewardRuleRepository.save(RewardRule.builder().id(BARCODE_INITIATIVEID_REWARDED)
                .initiativeConfig(InitiativeConfig.builder()
                        .initiativeId(BARCODE_INITIATIVEID_REWARDED)
                        .initiativeRewardType(InitiativeRewardType.DISCOUNT)
                        .startDate(TODAY.minusDays(1))
                        .endDate(TODAY.plusDays(1))
                        .build())
                .build());
        TransactionBarCodeCreationRequest trxRequest = TransactionBarCodeCreationRequest.builder()
                .initiativeId(BARCODE_INITIATIVEID_REWARDED)
                .build();
        AuthBarCodePaymentDTO authBarCodePaymentDTO = AuthBarCodePaymentDTO.builder().amountCents(10000L).build();

        // Creating transaction
        TransactionResponse trxCreated = extractResponse(createTrx(trxRequest, USERID), HttpStatus.CREATED, TransactionResponse.class);

        extractResponse(authTrx(trxCreated.getTrxCode(), authBarCodePaymentDTO, USERID), HttpStatus.OK, null);
    }

    @Test
    void authorizeTransaction_userStatusSuspended() throws Exception{
        final String USERID_SUSPENDED = "USERID_SUSPENDED";
        rewardRuleRepository.save(RewardRule.builder().id(INITIATIVEID)
                .initiativeConfig(InitiativeConfig.builder()
                        .initiativeId(INITIATIVEID)
                        .initiativeRewardType(InitiativeRewardType.DISCOUNT)
                        .startDate(TODAY.minusDays(1))
                        .endDate(TODAY.plusDays(1))
                        .build())
                .build());
        TransactionBarCodeCreationRequest trxRequest = TransactionBarCodeCreationRequest.builder()
                .initiativeId(INITIATIVEID)
                .build();
        AuthBarCodePaymentDTO authBarCodePaymentDTO = AuthBarCodePaymentDTO.builder().amountCents(10000L).build();

        // Creating transaction
        TransactionResponse trxCreated = extractResponse(createTrx(trxRequest, USERID_SUSPENDED), HttpStatus.CREATED, TransactionResponse.class);

        extractResponse(authTrx(trxCreated.getTrxCode(), authBarCodePaymentDTO, MERCHANTID), HttpStatus.FORBIDDEN, null);
    }

    @Test
    void createTransaction_invalidInitiativePeriod() throws Exception{
        rewardRuleRepository.save(RewardRule.builder().id(INITIATIVEID_NOT_STARTED)
                .initiativeConfig(InitiativeConfig.builder()
                        .initiativeId(INITIATIVEID_NOT_STARTED)
                        .initiativeRewardType(InitiativeRewardType.DISCOUNT)
                        .startDate(TODAY.plusDays(1))
                        .endDate(TODAY.plusDays(1))
                        .build())
                .build());

        TransactionBarCodeCreationRequest trxRequest = TransactionBarCodeCreationRequest.builder()
                .initiativeId(INITIATIVEID_NOT_STARTED)
                .build();

        // Creating transaction
        extractResponse(createTrx(trxRequest, USERID), HttpStatus.BAD_REQUEST, null);
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
