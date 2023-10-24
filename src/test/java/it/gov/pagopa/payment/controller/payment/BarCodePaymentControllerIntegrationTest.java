package it.gov.pagopa.payment.controller.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.BaseIntegrationTest;
import it.gov.pagopa.payment.connector.event.trx.dto.TransactionOutcomeDTO;
import it.gov.pagopa.payment.connector.event.trx.dto.mapper.TransactionInProgress2TransactionOutcomeDTOMapper;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.dto.mapper.TransactionBarCodeInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.enums.InitiativeRewardType;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.InitiativeConfig;
import it.gov.pagopa.payment.model.RewardRule;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.RewardConstants;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class BarCodePaymentControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RewardRuleRepository rewardRuleRepository;
    @Autowired
    private TransactionInProgressRepository transactionInProgressRepository;
    @Autowired
    private TransactionBarCodeInProgress2TransactionResponseMapper transactionResponseMapper;
    @Autowired
    private TransactionInProgress2TransactionOutcomeDTOMapper transactionInProgress2TransactionOutcomeDTOMapper;

    private static final String USERID = "USERID";
    private static final String USERID_UNSUBSCRIBED = "USERID_UNSUBSCRIBED";
    private static final String MERCHANTID = "MERCHANTID";
    private static final String INITIATIVEID = "INITIATIVEID";
    private static final String BARCODE_INITIATIVEID_REWARDED = "BARCODE_INITIATIVEID_REWARDED";
    private static final String BARCODE_INITIATIVEID_NO_BUDGET = "BARCODE_INITIATIVEID_NO_BUDGET";
    private static final String BARCODE_INITIATIVEID_TOOMANYREQUEST = "BARCODE_INITIATIVEID_TOOMANYREQUEST";
    private static final String INITIATIVEID_NOT_STARTED = INITIATIVEID + "1";
    private static final LocalDate TODAY = LocalDate.now();
    private final Set<TransactionOutcomeDTO> expectedAuthorizationNotificationEvents = Collections.synchronizedSet(new HashSet<>());

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
    void createTransaction_initiativeNotExistent() throws Exception {
        TransactionBarCodeCreationRequest trxRequest = TransactionBarCodeCreationRequest.builder()
                .initiativeId("DUMMYINITIATIVEID").build();
        String trxCode = "trxcode1";
        assertResponse(createTrx(trxRequest, USERID), HttpStatus.NOT_FOUND, null);

        AuthBarCodePaymentDTO authPaymentDTO = AuthBarCodePaymentDTO.builder().amountCents(10000L).build();
        assertResponse(authTrx(trxCode, authPaymentDTO, MERCHANTID), HttpStatus.NOT_FOUND, null);
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
        assertResponse(createTrx(trxRequest, USERID), HttpStatus.FORBIDDEN, null);
    }

    @Test
    void createTransaction_userNotOnboarded() throws Exception {
        saveInitiativeRewardRule(INITIATIVEID);

        TransactionBarCodeCreationRequest trxRequest = TransactionBarCodeCreationRequest.builder()
                .initiativeId(INITIATIVEID).build();

        // Creating transaction
        assertResponse(createTrx(trxRequest, "USERID_KO"), HttpStatus.FORBIDDEN, TransactionBarCodeResponse.class);
    }

    @Test
    void createTransaction_userStatusUnsubscribed() throws Exception{
        saveInitiativeRewardRule(INITIATIVEID);

        TransactionBarCodeCreationRequest trxRequest = TransactionBarCodeCreationRequest.builder()
                .initiativeId(INITIATIVEID)
                .build();

        // Creating transaction
        assertResponse(createTrx(trxRequest, USERID_UNSUBSCRIBED), HttpStatus.FORBIDDEN, null);
    }

    @Test
    void authorizeTransaction_TooManyRequestThrownByRewardCalculator() throws Exception {
        saveInitiativeRewardRule(BARCODE_INITIATIVEID_TOOMANYREQUEST);

        TransactionBarCodeCreationRequest trxRequest = TransactionBarCodeCreationRequest.builder()
                .initiativeId(BARCODE_INITIATIVEID_TOOMANYREQUEST).build();
        AuthBarCodePaymentDTO authPaymentDTO = AuthBarCodePaymentDTO.builder().amountCents(10000L).build();

        // Creating transaction
        TransactionBarCodeResponse trxCreated = createTrxSuccess(trxRequest, USERID);

        // Authorizing transaction but obtaining Too Many requests by reward-calculator
        assertResponse(authTrx(trxCreated.getTrxCode(), authPaymentDTO, MERCHANTID), HttpStatus.TOO_MANY_REQUESTS, null);
    }

    @Test
    void authorizeTransaction_budgetExhausted() throws Exception{
        saveInitiativeRewardRule(BARCODE_INITIATIVEID_NO_BUDGET);

        TransactionBarCodeCreationRequest trxRequest = TransactionBarCodeCreationRequest.builder()
                .initiativeId(BARCODE_INITIATIVEID_NO_BUDGET)
                .build();
        AuthBarCodePaymentDTO authBarCodePaymentDTO = AuthBarCodePaymentDTO.builder().amountCents(10000L).build();

        // Creating transaction
        TransactionBarCodeResponse trxCreated = createTrxSuccess(trxRequest, USERID);

        assertResponse(authTrx(trxCreated.getTrxCode(), authBarCodePaymentDTO, MERCHANTID), HttpStatus.FORBIDDEN, null);
    }

    @Test
    void authorizeTransaction_userStatusSuspended() throws Exception{
        saveInitiativeRewardRule(INITIATIVEID);

        TransactionBarCodeCreationRequest trxRequest = TransactionBarCodeCreationRequest.builder()
                .initiativeId(INITIATIVEID)
                .build();
        AuthBarCodePaymentDTO authBarCodePaymentDTO = AuthBarCodePaymentDTO.builder().amountCents(10000L).build();

        // Creating transaction
        TransactionBarCodeResponse trxCreated = createTrxSuccess(trxRequest, "USERID_SUSPENDED");

        assertResponse(authTrx(trxCreated.getTrxCode(), authBarCodePaymentDTO, MERCHANTID), HttpStatus.FORBIDDEN, null);
    }

    @Test
    void authorizeTransaction_userStatusUnsubscribed() throws Exception{
        saveInitiativeRewardRule(INITIATIVEID);

        TransactionBarCodeCreationRequest trxRequest = TransactionBarCodeCreationRequest.builder()
                .initiativeId(INITIATIVEID)
                .build();
        AuthBarCodePaymentDTO authBarCodePaymentDTO = AuthBarCodePaymentDTO.builder().amountCents(10000L).build();

        // Creating transaction
        TransactionBarCodeResponse trxCreated = createTrxSuccess(trxRequest, USERID);

        //Assigning the bar code to another user so that the auth throws the unsubscribed exception (through the proper stub)
        TransactionInProgress trxInProgress = transactionInProgressRepository.findById(trxCreated.getId()).orElse(null);
        trxInProgress.setUserId(USERID_UNSUBSCRIBED);
        transactionInProgressRepository.save(trxInProgress);

        assertResponse(authTrx(trxCreated.getTrxCode(), authBarCodePaymentDTO, MERCHANTID), HttpStatus.FORBIDDEN, null);
    }

    @Test
    void authorizeTransaction() throws Exception{
        saveInitiativeRewardRule(BARCODE_INITIATIVEID_REWARDED);

        TransactionBarCodeCreationRequest trxRequest = TransactionBarCodeCreationRequest.builder()
                .initiativeId(BARCODE_INITIATIVEID_REWARDED)
                .build();
        AuthBarCodePaymentDTO authBarCodePaymentDTO = AuthBarCodePaymentDTO.builder().amountCents(10000L).build();

        // Creating transaction
        TransactionBarCodeResponse trxCreated = createTrxSuccess(trxRequest, USERID);

        // Trying to authorize the bar code with a merchant not onboarded on the initiative
        assertResponse(authTrx(trxCreated.getTrxCode(), authBarCodePaymentDTO, "DUMMYMERCHANTID"), HttpStatus.FORBIDDEN, null);

        // Authroizing the bar code with a merchant onboarded on the initiative
        AuthPaymentDTO authPayment = assertResponse(authTrx(trxCreated.getTrxCode(), authBarCodePaymentDTO, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
        assertEquals(SyncTrxStatus.AUTHORIZED, authPayment.getStatus());
        assertAuthData(authPayment);

        addExpectedAuthorizationEvent(authPayment);
        checkNotificationEventsOnTransactionQueue();
    }

    private <T> T assertResponse(MvcResult response, HttpStatus expectedHttpStatusCode, Class<T> expectedBodyClass) {
        return TestUtils.assertResponse(response,expectedHttpStatusCode,expectedBodyClass);
    }

    private TransactionBarCodeResponse createTrxSuccess(TransactionBarCodeCreationRequest trxRequest, String userId) throws Exception {
        TransactionBarCodeResponse trxCreated = assertResponse(createTrx(trxRequest, userId), HttpStatus.CREATED, TransactionBarCodeResponse.class);
        assertEquals(SyncTrxStatus.CREATED, trxCreated.getStatus());
        checkTransactionStored(trxCreated);
        assertTrxCreatedData(trxRequest, trxCreated, userId);
        return trxCreated;
    }

    private void checkTransactionStored(TransactionBarCodeResponse trxCreated){
        TransactionInProgress stored = checkIfStored(trxCreated.getId());

        assertEquals(RewardConstants.TRX_CHANNEL_BARCODE, stored.getChannel());
        trxCreated.setTrxDate(OffsetDateTime.parse(
                trxCreated.getTrxDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx"))));
        assertEquals(trxCreated, transactionResponseMapper.apply(stored));
    }

    private TransactionInProgress checkIfStored(String trxId) {
        TransactionInProgress stored = transactionInProgressRepository.findById(trxId).orElse(null);
        assertNotNull(stored);
        return stored;
    }

    private void assertTrxCreatedData(TransactionBarCodeCreationRequest trxRequest, TransactionBarCodeResponse trxCreated, String userId) {
        assertCommonFields(trxRequest, trxCreated);

        TransactionInProgress stored = transactionInProgressRepository.findById(trxCreated.getId()).orElse(null);
        assertNotNull(stored);
        assertCommonFields(trxCreated, stored, userId);
    }

    private void assertCommonFields(TransactionBarCodeCreationRequest trxRequest, TransactionBarCodeResponse trxResponse) {
        OffsetDateTime now = OffsetDateTime.now();
        assertNotNull(trxResponse.getId());
        assertNotNull(trxResponse.getTrxCode());
        Assertions.assertEquals(trxRequest.getInitiativeId(), trxResponse.getInitiativeId());
        Assertions.assertFalse(trxResponse.getTrxDate().isAfter(now.plusMinutes(1L)));
        Assertions.assertFalse(trxResponse.getTrxDate().isBefore(now.minusMinutes(1L)));
        Assertions.assertEquals(SyncTrxStatus.CREATED, trxResponse.getStatus());
    }

    private void assertCommonFields(TransactionBarCodeResponse trxResponse, TransactionInProgress trxStored, String userId) {
        Assertions.assertEquals(trxResponse.getId(), trxStored.getId());
        Assertions.assertEquals(trxResponse.getTrxCode(), trxStored.getTrxCode());
        Assertions.assertEquals(trxResponse.getTrxDate(), trxStored.getTrxDate());
        Assertions.assertEquals("00", trxStored.getOperationType());
        Assertions.assertEquals(OperationType.CHARGE, trxStored.getOperationTypeTranscoded());
        Assertions.assertEquals(trxResponse.getId(), trxStored.getCorrelationId());
        Assertions.assertEquals(trxResponse.getInitiativeId(), trxStored.getInitiativeId());
        Assertions.assertEquals(userId, trxStored.getUserId());
        Assertions.assertEquals(trxResponse.getStatus(), trxStored.getStatus());
        Assertions.assertEquals(RewardConstants.TRX_CHANNEL_BARCODE, trxStored.getChannel());
    }

    private void assertAuthData(AuthPaymentDTO authResult) {
        TransactionInProgress stored = transactionInProgressRepository.findById(authResult.getId()).orElse(null);
        assertNotNull(stored);
        assertCommonFields(authResult, stored);
    }

    private void assertCommonFields(AuthPaymentDTO authPaymentDTO, TransactionInProgress trxStored) {
        Assertions.assertEquals(authPaymentDTO.getId(), trxStored.getId());
        Assertions.assertEquals(authPaymentDTO.getTrxCode(), trxStored.getTrxCode());
        Assertions.assertEquals(authPaymentDTO.getTrxDate(), trxStored.getTrxDate());
        Assertions.assertEquals("00", trxStored.getOperationType());
        Assertions.assertEquals(OperationType.CHARGE, trxStored.getOperationTypeTranscoded());
        Assertions.assertEquals("EUR", trxStored.getAmountCurrency());
        Assertions.assertEquals(MERCHANTID, trxStored.getMerchantId());
        Assertions.assertEquals(authPaymentDTO.getInitiativeId(), trxStored.getInitiativeId());
        Assertions.assertEquals(USERID, trxStored.getUserId());
        Assertions.assertEquals(authPaymentDTO.getStatus(), trxStored.getStatus());
        Assertions.assertEquals(SyncTrxStatus.AUTHORIZED, trxStored.getStatus());
        Assertions.assertEquals(RewardConstants.TRX_CHANNEL_BARCODE, trxStored.getChannel());
    }

    private void addExpectedAuthorizationEvent(AuthPaymentDTO authPaymentDTO) {
        TransactionInProgress trxAuth = checkIfStored(authPaymentDTO.getId());
        trxAuth.setAmountCents(authPaymentDTO.getAmountCents());
        trxAuth.setEffectiveAmount(CommonUtilities.centsToEuro(authPaymentDTO.getAmountCents()));
        trxAuth.setBusinessName("BUSINESSNAME");
        Assertions.assertNotEquals(Collections.emptyMap(), trxAuth.getRewards());
        expectedAuthorizationNotificationEvents.add(transactionInProgress2TransactionOutcomeDTOMapper.apply(trxAuth));
    }

    private void checkNotificationEventsOnTransactionQueue() {
        int numExpectedNotification =
                expectedAuthorizationNotificationEvents.size();
        List<ConsumerRecord<String, String>> consumerRecords = kafkaTestUtilitiesService.consumeMessages(topicConfirmNotification, numExpectedNotification, 15000);

        Map<SyncTrxStatus, Set<TransactionOutcomeDTO>> eventsResult = consumerRecords.stream()
                .map(r -> {
                    TransactionOutcomeDTO out = TestUtils.jsonDeserializer(r.value(), TransactionOutcomeDTO.class);
                    System.out.printf("OUTCOME DTO: " + out);
                    return out;
                })
                .collect(Collectors.groupingBy(TransactionOutcomeDTO::getStatus, Collectors.toSet()));

        checkAuthorizationNotificationEvents(eventsResult.get(SyncTrxStatus.AUTHORIZED));
    }

    private void checkAuthorizationNotificationEvents(Set<TransactionOutcomeDTO> authorizationNotificationDTOS) {
        assertNotifications(expectedAuthorizationNotificationEvents, authorizationNotificationDTOS);
    }

    private void assertNotifications(Set<TransactionOutcomeDTO> expectedNotificationEvents, Set<TransactionOutcomeDTO> notificationDTOS) {
        expectedNotificationEvents.stream().filter(n -> n.getElaborationDateTime() != null).forEach(e -> e.setElaborationDateTime(TestUtils.truncateTimestamp(e.getElaborationDateTime())));
        notificationDTOS.stream().filter(n -> n.getElaborationDateTime() != null).forEach(e -> e.setElaborationDateTime(TestUtils.truncateTimestamp(e.getElaborationDateTime())));
        assertEquals(expectedNotificationEvents.size(), notificationDTOS.size());
        assertEquals(
                sortEvents(expectedNotificationEvents),
                sortEvents(notificationDTOS)
        );
    }

    private List<TransactionOutcomeDTO> sortEvents(Set<TransactionOutcomeDTO> list) {
        return list.stream()
                .sorted(Comparator.comparing(TransactionOutcomeDTO::getId))
                .toList();
    }

    private void saveInitiativeRewardRule(String initiativeId){
        rewardRuleRepository.save(RewardRule.builder().id(initiativeId)
                .initiativeConfig(InitiativeConfig.builder()
                        .initiativeId(initiativeId)
                        .initiativeRewardType(InitiativeRewardType.DISCOUNT)
                        .startDate(TODAY.minusDays(1))
                        .endDate(TODAY.plusDays(1))
                        .build())
                .build());
    }
}
