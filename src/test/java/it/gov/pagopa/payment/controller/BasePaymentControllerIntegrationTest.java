package it.gov.pagopa.payment.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.payment.BaseIntegrationTest;
import it.gov.pagopa.payment.connector.event.producer.dto.AuthorizationNotificationDTO;
import it.gov.pagopa.payment.connector.event.producer.mapper.AuthorizationNotificationMapper;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.mapper.TransactionCreationRequest2TransactionInProgressMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2SyncTrxStatusMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.RewardRule;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.TransactionCreationRequestFaker;
import it.gov.pagopa.payment.test.utils.TestUtils;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@TestPropertySource(
        properties = {
                "logging.level.it.gov.pagopa.payment=WARN",
                "logging.level.it.gov.pagopa.common=WARN",
                "logging.level.it.gov.pagopa.payment.exception.ErrorManager=WARN",
                "app.qrCode.throttlingSeconds=2"
        })
abstract class BasePaymentControllerIntegrationTest extends BaseIntegrationTest {

    public static final String INITIATIVEID = "INITIATIVEID";
    public static final String USERID = "USERID";
    public static final String MERCHANTID = "MERCHANTID";
    public static final String ACQUIRERID = "ACQUIRERID";
    public static final String IDTRXACQUIRER = "IDTRXACQUIRER";

    private static final int parallelism = 8;
    private static final ExecutorService executor = Executors.newFixedThreadPool(parallelism);

    private final List<FailableConsumer<Integer, Exception>> useCases = new ArrayList<>();

    private final Set<AuthorizationNotificationDTO> expectedAuthorizationNotificationEvents = Collections.synchronizedSet(new HashSet<>());
    private final Set<AuthorizationNotificationDTO> expectedAuthorizationNotificationEventsRejected = Collections.synchronizedSet(new HashSet<>());
    private final Set<TransactionInProgress> expectedConfirmNotificationEvents= Collections.synchronizedSet(new HashSet<>());

    @Autowired
    private RewardRuleRepository rewardRuleRepository;
    @Autowired
    private TransactionInProgressRepository transactionInProgressRepository;

    @Autowired
    private TransactionInProgress2TransactionResponseMapper transactionResponseMapper;
    @Autowired
    private TransactionInProgress2SyncTrxStatusMapper transactionInProgress2SyncTrxStatusMapper;
    @Autowired
    private AuthorizationNotificationMapper authorizationNotificationMapper;
    @Autowired
    private TransactionCreationRequest2TransactionInProgressMapper transactionCreationRequest2TransactionInProgressMapper;

    @Value("${app.qrCode.throttlingSeconds}")
    private int throttlingSeconds;

    @Test
    void test() {
        int N = Math.max(useCases.size(), 50);

        rewardRuleRepository.save(RewardRule.builder().id(INITIATIVEID).build());

        List<? extends Future<?>> tasks = IntStream.range(0, N)
                .mapToObj(i -> executor.submit(() -> {
                    try {
                        useCases.get(i % useCases.size()).accept(i);
                    } catch (Exception e) {
                        throw new IllegalStateException("Unexpected exception thrown during test", e);
                    }
                }))
                .toList();

        for (int i = 0; i < tasks.size(); i++) {
            try {
                tasks.get(i).get();
            } catch (Exception e) {
                System.err.printf("UseCase %d (bias %d) failed: %n", i % useCases.size(), i);
                if(e instanceof RuntimeException runtimeException){
                    throw runtimeException;
                } else if(e.getCause() instanceof AssertionFailedError assertionFailedError){
                    throw assertionFailedError;
                }
                Assertions.fail(e);
            }
        }

        // Verifying authorization event notification
        checkAuthorizationNotificationEvents();

        //Verifying confirm event notification
        checkConfirmNotificationEvents();

        //verifying error event notification
        //checkErrorNotificationEvents(); TODO complete error publisher useCase
    }

    /** Controller's channel */
    protected abstract String getChannel();

    /**
     * Invoke create transaction API acting as <i>merchantId</i>
     */
    protected abstract MvcResult createTrx(TransactionCreationRequest trxRequest, String merchantId, String acquirerId, String idTrxAcquirer) throws Exception;

    /**
     * Invoke pre-authorize transaction API to relate <i>userId</i> to the transaction created by <i>merchantId</i>
     */
    protected abstract MvcResult preAuthTrx(TransactionResponse trx, String userid, String merchantId) throws Exception;

    /**
     * Invoke pre-authorize transaction API to authorize the transaction acting as <i>userId</i> to the transaction created by <i>merchantId</i>
     */
    protected abstract MvcResult authTrx(TransactionResponse trx, String userid, String merchantId) throws Exception;

    /**
     * Invoke confirm payment API acting as <i>merchantId</i>
     */
    protected abstract MvcResult confirmPayment(TransactionResponse trx, String merchantId, String acquirerId) throws Exception;

    /**
     * Invoke getStatusTransaction API acting as <i>merchantId</i>
     */
    protected abstract MvcResult getStatusTransaction(String transactionId, String merchantId, String acquirerId) throws Exception;

    /**
     * Override in order to add specific use cases
     */
    protected List<FailableConsumer<Integer, Exception>> getExtraUseCases() {
        return Collections.emptyList();
    }

    private TransactionResponse createTrxSuccess(TransactionCreationRequest trxRequest) throws Exception {
        TransactionResponse trxCreated = extractResponse(createTrx(trxRequest, MERCHANTID, ACQUIRERID, IDTRXACQUIRER), HttpStatus.CREATED, TransactionResponse.class);
        Assertions.assertEquals(SyncTrxStatus.CREATED, trxCreated.getStatus());
        checkTransactionStored(trxCreated);
        return trxCreated;
    }

    private void checkTransactionStored(TransactionResponse trxCreated) throws Exception {
        TransactionInProgress stored = checkIfStored(trxCreated.getId());
        // Authorized merchant
        SyncTrxStatusDTO syncTrxStatusResult =extractResponse(getStatusTransaction(trxCreated.getId(), trxCreated.getMerchantId(), trxCreated.getAcquirerId()),HttpStatus.OK, SyncTrxStatusDTO.class);
        Assertions.assertEquals(transactionInProgress2SyncTrxStatusMapper.transactionInProgressMapper(stored),syncTrxStatusResult);
        //Unauthorized operator
        extractResponse(getStatusTransaction(trxCreated.getId(), "DUMMYMERCHANTID", trxCreated.getAcquirerId()),HttpStatus.NOT_FOUND, null);

        Assertions.assertEquals(getChannel(), stored.getChannel());
        Assertions.assertEquals(trxCreated, transactionResponseMapper.apply(stored));
    }

    private void checkTransactionStored(AuthPaymentDTO trx, String expectedUserId) {
        TransactionInProgress stored = checkIfStored(trx.getId());

        Assertions.assertEquals(trx.getId(), stored.getId());
        Assertions.assertEquals(trx.getTrxCode(), stored.getTrxCode());
        Assertions.assertEquals(trx.getInitiativeId(), stored.getInitiativeId());
        Assertions.assertEquals(trx.getStatus(), stored.getStatus());
        Assertions.assertEquals(trx.getReward(), stored.getReward());
        Assertions.assertEquals(trx.getRejectionReasons(), stored.getRejectionReasons());

        Assertions.assertEquals(expectedUserId, stored.getUserId());
    }

    private TransactionInProgress checkIfStored(String trxId) {
        TransactionInProgress stored = transactionInProgressRepository.findById(trxId).orElse(null);
        Assertions.assertNotNull(stored);
        return stored;
    }

    {
        // useCase 0: initiative not existent
        useCases.add(i -> {
            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId("DUMMYINITIATIVEID");

            extractResponse(createTrx(trxRequest, MERCHANTID, ACQUIRERID, IDTRXACQUIRER), HttpStatus.NOT_FOUND, null);

            // Other APIs cannot be invoked because we have not a valid trxId
            TransactionResponse dummyTrx = TransactionResponse.builder().id("DUMMYTRXID").trxCode("dummytrxcode").trxDate(OffsetDateTime.now()).build();
            extractResponse(preAuthTrx(dummyTrx, USERID, MERCHANTID), HttpStatus.NOT_FOUND, null);
            extractResponse(authTrx(dummyTrx, USERID, MERCHANTID), HttpStatus.NOT_FOUND, null);
            extractResponse(confirmPayment(dummyTrx, MERCHANTID, ACQUIRERID), HttpStatus.NOT_FOUND, null);
        });

        // useCase 1: userId not onboarded
        useCases.add(i -> {
            String userIdNotOnboarded = "DUMMYUSERID";

            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId(INITIATIVEID);

            // Creating transaction
            TransactionResponse trxCreated = createTrxSuccess(trxRequest);

            // Cannot relate user because not onboarded
            AuthPaymentDTO failedPreview = extractResponse(preAuthTrx(trxCreated, userIdNotOnboarded, MERCHANTID), HttpStatus.FORBIDDEN, AuthPaymentDTO.class);
            failedPreview.setReward(null);
            Assertions.assertEquals(SyncTrxStatus.REJECTED, failedPreview.getStatus());
            Assertions.assertEquals(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE), failedPreview.getRejectionReasons());
            extractResponse(preAuthTrx(trxCreated, userIdNotOnboarded, MERCHANTID), HttpStatus.BAD_REQUEST, null);

            // Other APIs will fail because status not expected
            extractResponse(authTrx(trxCreated, userIdNotOnboarded, MERCHANTID), HttpStatus.BAD_REQUEST, null);
            extractResponse(confirmPayment(trxCreated, MERCHANTID, ACQUIRERID), HttpStatus.BAD_REQUEST, null);

            checkTransactionStored(failedPreview, userIdNotOnboarded);
        });

        // useCase 2: trx rejected
        useCases.add(i -> {
            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId(INITIATIVEID);
            trxRequest.setMcc("NOTALLOWEDMCC");

            // Creating transaction
            TransactionResponse trxCreated = createTrxSuccess(trxRequest);

            // Relating to user
            AuthPaymentDTO preAuthResult = extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
            preAuthResult.setReward(null);
            Assertions.assertEquals(SyncTrxStatus.REJECTED, preAuthResult.getStatus());
            checkTransactionStored(preAuthResult, USERID);

            // Cannot invoke other APIs if REJECTED
            extractResponse(authTrx(trxCreated, USERID, MERCHANTID), HttpStatus.BAD_REQUEST, null);
            extractResponse(confirmPayment(trxCreated, MERCHANTID, ACQUIRERID), HttpStatus.BAD_REQUEST, null);
        });

        // useCase 3: trx rejected when authorizing
        useCases.add(i -> {
            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId(INITIATIVEID);

            // Creating transaction
            TransactionResponse trxCreated = createTrxSuccess(trxRequest);

            // Relating to user
            AuthPaymentDTO preAuthResult = extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
            Assertions.assertEquals(SyncTrxStatus.IDENTIFIED, preAuthResult.getStatus());
            preAuthResult.setReward(null);
            checkTransactionStored(preAuthResult, USERID);

            // Authorizing transaction, but obtaining rejection
            updateStoredTransaction(preAuthResult.getId(), t -> t.setMcc("NOTALLOWEDMCC"));
            AuthPaymentDTO authResult = extractResponse(authTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
            Assertions.assertEquals(SyncTrxStatus.REJECTED, authResult.getStatus());
            checkTransactionStored(authResult, USERID);

            //setpayload authResultRejected
            addExpectedAuthorizationEventRejected(trxCreated, authResult);
        });

        // useCase 4: TooMany request thrown by reward-calculator
        useCases.add(i -> {
            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId(INITIATIVEID);

            // Creating transaction
            TransactionResponse trxCreated = createTrxSuccess(trxRequest);

            // Relating to user
            AuthPaymentDTO preAuthResult = extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
            Assertions.assertEquals(SyncTrxStatus.IDENTIFIED, preAuthResult.getStatus());
            preAuthResult.setReward(null);
            checkTransactionStored(preAuthResult, USERID);

            // Authorizing transaction but obataining Too Many requests by reward-calculator
            updateStoredTransaction(preAuthResult.getId(), t -> t.setVat("TOOMANYREQUESTS"));
            extractResponse(authTrx(trxCreated, USERID, MERCHANTID), HttpStatus.TOO_MANY_REQUESTS, null);
            checkTransactionStored(preAuthResult, USERID);
        });

        // useCase 5: complete successful flow
        useCases.add(i -> {
            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId(INITIATIVEID);

            // Creating transaction
            TransactionResponse trxCreated = createTrxSuccess(trxRequest);

            // Cannot invoke other APIs if not relating first
            extractResponse(authTrx(trxCreated, USERID, MERCHANTID), HttpStatus.BAD_REQUEST, null);
            extractResponse(confirmPayment(trxCreated, MERCHANTID, ACQUIRERID), HttpStatus.BAD_REQUEST, null);
            waitThrottlingTime();

            // Relating to user
            AuthPaymentDTO preAuthResult = extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
            Assertions.assertEquals(SyncTrxStatus.IDENTIFIED, preAuthResult.getStatus());
            // Relating to user resubmission
            AuthPaymentDTO preAuthResultResubmitted = extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
            Assertions.assertEquals(preAuthResult, preAuthResultResubmitted);
            preAuthResult.setReward(null);
            checkTransactionStored(preAuthResult, USERID);
            // Only the right userId could resubmit preview
            extractResponse(preAuthTrx(trxCreated, "DUMMYUSERID", MERCHANTID), HttpStatus.FORBIDDEN, null);


            // Cannot invoke other APIs if not authorizing first
            extractResponse(confirmPayment(trxCreated, MERCHANTID, ACQUIRERID), HttpStatus.BAD_REQUEST, null);

            // Only the right userId could authorize its transaction
            extractResponse(authTrx(trxCreated, "DUMMYUSERID", MERCHANTID), HttpStatus.FORBIDDEN, null);

            waitThrottlingTime();

            // Authorizing transaction
            AuthPaymentDTO authResult = extractResponse(authTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
            // TooManyRequest behavior
            extractResponse(authTrx(trxCreated, USERID, MERCHANTID), HttpStatus.TOO_MANY_REQUESTS, null);
            Assertions.assertEquals(SyncTrxStatus.AUTHORIZED, authResult.getStatus());
            // Cannot invoke preAuth after authorization
            extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), HttpStatus.BAD_REQUEST, null);
            // Authorizing transaction resubmission after throttling time
            waitThrottlingTime();
            updateStoredTransaction(authResult.getId(), t -> t.setCorrelationId("ALREADY_AUTHORED"));
            AuthPaymentDTO authResultResubmitted = extractResponse(authTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
            Assertions.assertEquals(authResult, authResultResubmitted);
            checkTransactionStored(authResult, USERID);

            //setpayload authResult
            addExpectedAuthorizationEvent(trxCreated, authResult);

            // Unexpected merchant trying to confirm
            extractResponse(confirmPayment(trxCreated, "DUMMYMERCHANTID", "DUMMYACQUIRERID"), HttpStatus.FORBIDDEN, null);
            waitThrottlingTime();

            //set payload confirm
            addExpectedConfirmEvent(trxCreated);

            // Confirming payment
            TransactionResponse confirmResult = extractResponse(confirmPayment(trxCreated, MERCHANTID, ACQUIRERID), HttpStatus.OK, TransactionResponse.class);
            Assertions.assertEquals(SyncTrxStatus.REWARDED, confirmResult.getStatus());

            // Confirming payment resubmission
            extractResponse(confirmPayment(trxCreated, MERCHANTID, ACQUIRERID), HttpStatus.NOT_FOUND, null);

            Assertions.assertFalse(transactionInProgressRepository.existsById(trxCreated.getId()));
        });

        //andare a verificare il messaggio è presente correttamente
        useCases.addAll(getExtraUseCases());
    }

    private void addExpectedAuthorizationEvent(TransactionResponse trxCreated, AuthPaymentDTO authResult) {
        expectedAuthorizationNotificationEvents.add(authorizationNotificationMapper.map(checkIfStored(trxCreated.getId()), authResult));
    }

    private void addExpectedAuthorizationEventRejected(TransactionResponse trxCreated, AuthPaymentDTO authResult) {
        expectedAuthorizationNotificationEventsRejected.add(authorizationNotificationMapper.map(checkIfStored(trxCreated.getId()), authResult));
    }

    private void addExpectedConfirmEvent(TransactionResponse trx){
        TransactionInProgress transactionConfirmed= checkIfStored(trx.getId());
        transactionConfirmed.setStatus(SyncTrxStatus.REWARDED);
        expectedConfirmNotificationEvents.add(transactionConfirmed);
    }

    private void waitThrottlingTime() {
        wait(throttlingSeconds, TimeUnit.SECONDS);
    }

    protected void updateStoredTransaction(String trxId, Consumer<TransactionInProgress> updater) {
        TransactionInProgress stored = checkIfStored(trxId);
        updater.accept(stored);
        transactionInProgressRepository.save(stored);
    }

    protected <T> T extractResponse(MvcResult response, HttpStatus expectedHttpStatusCode, Class<T> expectedBodyClass) {
        Assertions.assertEquals(expectedHttpStatusCode.value(), response.getResponse().getStatus());
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

    private void checkAuthorizationNotificationEvents() {
        Set<AuthorizationNotificationDTO> expectedNotificationEvents = new HashSet<>();
        expectedNotificationEvents.addAll(expectedAuthorizationNotificationEvents);
        expectedNotificationEvents.addAll(expectedAuthorizationNotificationEventsRejected);
        int expectedNumNotificationEvents = expectedNotificationEvents.size();

        Map<String, AuthorizationNotificationDTO> trxId2AuthEvent = expectedNotificationEvents.stream()
                .collect(Collectors.toMap(AuthorizationNotificationDTO::getTrxId, Function.identity()));
        List<ConsumerRecord<String, String>> consumerRecords = consumeMessages(topicAuthorizationNotification, expectedNumNotificationEvents, 15000);
        Assertions.assertEquals(expectedNumNotificationEvents, consumerRecords.size());

        Map<SyncTrxStatus, Set<AuthorizationNotificationDTO>> eventsResult = consumerRecords.stream()
                .map(r -> {
                    AuthorizationNotificationDTO out = TestUtils.jsonDeserializer(r.value(), AuthorizationNotificationDTO.class);
                    Assertions.assertEquals(out.getUserId(), r.key());
                    checkAuthorizationDateTime(trxId2AuthEvent, out);

                    return out;
                })
                .collect(Collectors.groupingBy(AuthorizationNotificationDTO::getStatus, Collectors.toSet()));

        Assertions.assertEquals(
                sortAuthorizationEvents(expectedAuthorizationNotificationEvents),
                sortAuthorizationEvents(eventsResult.get(SyncTrxStatus.AUTHORIZED))
        );
        Assertions.assertEquals(
                sortAuthorizationEvents(expectedAuthorizationNotificationEventsRejected),
                sortAuthorizationEvents(eventsResult.get(SyncTrxStatus.REJECTED))
        );
    }

    private void  checkConfirmNotificationEvents(){
        int expectedNotificationEvents= expectedConfirmNotificationEvents.size();
        List<ConsumerRecord<String,String>> consumerRecords= consumeMessages(topicConfirmNotification, expectedNotificationEvents,15000);
        Assertions.assertEquals(expectedNotificationEvents,consumerRecords.size());

        Set<TransactionInProgress> eventResult= consumerRecords.stream()
                .map(r ->{
                    TransactionInProgress out= TestUtils.jsonDeserializer(r.value(), TransactionInProgress.class);
                    Assertions.assertEquals(out.getMerchantId(), r.key());

                    return out;
                })
                .collect(Collectors.toSet());

        Assertions.assertEquals(
                sortConfirmEvents(expectedConfirmNotificationEvents),
                sortConfirmEvents(eventResult)
        );
    }

    private void checkErrorNotificationEvents() {
        int expectedNotificationEvents = expectedAuthorizationNotificationEventsRejected.size();
        Map<String, AuthorizationNotificationDTO> trxId2AuthEvent = expectedAuthorizationNotificationEvents.stream()
                .collect(Collectors.toMap(AuthorizationNotificationDTO::getTrxId, Function.identity()));
        List<ConsumerRecord<String,String>> consumerRecords = consumeMessages(topicErrors, expectedNotificationEvents,15000);
        Assertions.assertEquals(expectedNotificationEvents,consumerRecords.size());

        Set<AuthorizationNotificationDTO> eventsResult = consumerRecords.stream()
                .map(r -> {
                    AuthorizationNotificationDTO out = TestUtils.jsonDeserializer(r.value(), AuthorizationNotificationDTO.class);
                    Assertions.assertEquals(out.getUserId(), r.key());
                    checkAuthorizationDateTime(trxId2AuthEvent, out);
                    checkErrorMessageHeaders(topicAuthorizationNotification, null, r, "TODO", "TODO", out.getUserId());

                    return out;
                })
                .collect(Collectors.toSet());

        Assertions.assertEquals(
                sortAuthorizationEvents(expectedAuthorizationNotificationEvents),
                sortAuthorizationEvents(eventsResult)
        );
    }

    private void checkAuthorizationDateTime(Map<String, AuthorizationNotificationDTO> trxId2AuthEvent, AuthorizationNotificationDTO out) {
        if (!out.getStatus().equals(SyncTrxStatus.REJECTED)) {
            AuthorizationNotificationDTO expectedEvent = trxId2AuthEvent.get(out.getTrxId());
            Assertions.assertNotNull(expectedEvent);
            Duration diffAuthDateTime = Duration.between(out.getAuthorizationDateTime(),
                    expectedEvent.getAuthorizationDateTime());
            Assertions.assertTrue(diffAuthDateTime
                    .compareTo(Duration.ofSeconds(throttlingSeconds)) >= 0);
            Assertions.assertTrue(diffAuthDateTime
                    .compareTo(Duration.ofSeconds(10L * throttlingSeconds)) < 0);
            out.setAuthorizationDateTime(expectedEvent.getAuthorizationDateTime());
        }
    }

    @NotNull
    private List<AuthorizationNotificationDTO> sortAuthorizationEvents(Set<AuthorizationNotificationDTO> list) {
        return list.stream()
                .sorted(Comparator.comparing(AuthorizationNotificationDTO::getTrxId))
                .toList();
    }

    @NotNull
    private List<TransactionInProgress> sortConfirmEvents(Set<TransactionInProgress> list) {
        return list.stream()
                .sorted(Comparator.comparing(TransactionInProgress::getMerchantFiscalCode))
                .toList();
    }

}
