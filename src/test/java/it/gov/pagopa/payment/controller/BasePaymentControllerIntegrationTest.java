package it.gov.pagopa.payment.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.payment.BaseIntegrationTest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private final Set<TransactionInProgress> expectedAuthorizationNotificationEvents = Collections.synchronizedSet(new HashSet<>());
    private final Set<TransactionInProgress> expectedAuthorizationNotificationRejectedEvents = Collections.synchronizedSet(new HashSet<>());
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

        checkNotificationEventsOnTransactionQueue();

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
        assertEquals(SyncTrxStatus.CREATED, trxCreated.getStatus());
        checkTransactionStored(trxCreated);
        return trxCreated;
    }

    private void checkTransactionStored(TransactionResponse trxCreated) throws Exception {
        TransactionInProgress stored = checkIfStored(trxCreated.getId());
        // Authorized merchant
        SyncTrxStatusDTO syncTrxStatusResult =extractResponse(getStatusTransaction(trxCreated.getId(), trxCreated.getMerchantId(), trxCreated.getAcquirerId()),HttpStatus.OK, SyncTrxStatusDTO.class);
        assertEquals(transactionInProgress2SyncTrxStatusMapper.transactionInProgressMapper(stored),syncTrxStatusResult);
        //Unauthorized operator
        extractResponse(getStatusTransaction(trxCreated.getId(), "DUMMYMERCHANTID", trxCreated.getAcquirerId()),HttpStatus.NOT_FOUND, null);

        assertEquals(getChannel(), stored.getChannel());
        assertEquals(trxCreated, transactionResponseMapper.apply(stored));
    }

    private void checkTransactionStored(AuthPaymentDTO trx, String expectedUserId) {
        TransactionInProgress stored = checkIfStored(trx.getId());

        assertEquals(trx.getId(), stored.getId());
        assertEquals(trx.getTrxCode(), stored.getTrxCode());
        assertEquals(trx.getInitiativeId(), stored.getInitiativeId());
        assertEquals(trx.getStatus(), stored.getStatus());
        assertEquals(trx.getReward(), stored.getReward());
        assertEquals(trx.getRejectionReasons(), stored.getRejectionReasons());

        assertEquals(expectedUserId, stored.getUserId());
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
            assertEquals(SyncTrxStatus.REJECTED, failedPreview.getStatus());
            assertEquals(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE), failedPreview.getRejectionReasons());
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
            assertEquals(SyncTrxStatus.REJECTED, preAuthResult.getStatus());
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
            assertEquals(SyncTrxStatus.IDENTIFIED, preAuthResult.getStatus());
            preAuthResult.setReward(null);
            checkTransactionStored(preAuthResult, USERID);

            // Authorizing transaction, but obtaining rejection
            updateStoredTransaction(preAuthResult.getId(), t -> t.setMcc("NOTALLOWEDMCC"));
            AuthPaymentDTO authResult = extractResponse(authTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
            assertEquals(SyncTrxStatus.REJECTED, authResult.getStatus());
            checkTransactionStored(authResult, USERID);

            //setpayload authResultRejected
            addExpectedAuthorizationEventRejected(trxCreated);
        });

        // useCase 4: TooMany request thrown by reward-calculator
        useCases.add(i -> {
            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId(INITIATIVEID);

            // Creating transaction
            TransactionResponse trxCreated = createTrxSuccess(trxRequest);

            // Relating to user
            AuthPaymentDTO preAuthResult = extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
            assertEquals(SyncTrxStatus.IDENTIFIED, preAuthResult.getStatus());
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
            assertEquals(SyncTrxStatus.IDENTIFIED, preAuthResult.getStatus());
            // Relating to user resubmission
            AuthPaymentDTO preAuthResultResubmitted = extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
            assertEquals(preAuthResult, preAuthResultResubmitted);
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
            assertEquals(SyncTrxStatus.AUTHORIZED, authResult.getStatus());
            // Cannot invoke preAuth after authorization
            extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), HttpStatus.BAD_REQUEST, null);
            // Authorizing transaction resubmission after throttling time
            waitThrottlingTime();

            //setpayload authResult
            addExpectedAuthorizationEvent(trxCreated);

            updateStoredTransaction(authResult.getId(), t -> t.setCorrelationId("ALREADY_AUTHORED"));
            AuthPaymentDTO authResultResubmitted = extractResponse(authTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
            assertEquals(authResult, authResultResubmitted);
            checkTransactionStored(authResult, USERID);

            // Unexpected merchant trying to confirm
            extractResponse(confirmPayment(trxCreated, "DUMMYMERCHANTID", "DUMMYACQUIRERID"), HttpStatus.FORBIDDEN, null);
            waitThrottlingTime();

            //set payload confirm
            addExpectedConfirmEvent(trxCreated);

            // Confirming payment
            TransactionResponse confirmResult = extractResponse(confirmPayment(trxCreated, MERCHANTID, ACQUIRERID), HttpStatus.OK, TransactionResponse.class);
            assertEquals(SyncTrxStatus.REWARDED, confirmResult.getStatus());

            // Confirming payment resubmission
            extractResponse(confirmPayment(trxCreated, MERCHANTID, ACQUIRERID), HttpStatus.NOT_FOUND, null);

            Assertions.assertFalse(transactionInProgressRepository.existsById(trxCreated.getId()));
        });

        //andare a verificare il messaggio è presente correttamente
        useCases.addAll(getExtraUseCases());
    }

    private void addExpectedAuthorizationEvent(TransactionResponse trx) {
        TransactionInProgress trxAuth = checkIfStored(trx.getId());
        trxAuth.setReward(null);
        expectedAuthorizationNotificationEvents.add(trxAuth);
    }

    private void addExpectedAuthorizationEventRejected(TransactionResponse trx) {
        TransactionInProgress trxRejected = checkIfStored(trx.getId());
        trxRejected.setReward(null);
        expectedAuthorizationNotificationRejectedEvents.add(trxRejected);
    }

    private void addExpectedConfirmEvent(TransactionResponse trx){
        TransactionInProgress transactionConfirmed = checkIfStored(trx.getId());
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

    private void checkNotificationEventsOnTransactionQueue() {
        int numExpectedNotification =
                expectedAuthorizationNotificationEvents.size() +
                        expectedAuthorizationNotificationRejectedEvents.size() +
                        expectedConfirmNotificationEvents.size();
        List<ConsumerRecord<String, String>> consumerRecords = consumeMessages(topicConfirmNotification, numExpectedNotification, 15000);

        Map<SyncTrxStatus, Set<TransactionInProgress>> eventsResult = consumerRecords.stream()
                .map(r -> {
                    TransactionInProgress out = TestUtils.jsonDeserializer(r.value(), TransactionInProgress.class);
                    if (out.getStatus().equals(SyncTrxStatus.REWARDED)) {
                        assertEquals(out.getMerchantId(), r.key());
                    } else {
                        assertEquals(out.getUserId(), r.key());
                    }

                    return out;
                })
                .collect(Collectors.groupingBy(TransactionInProgress::getStatus, Collectors.toSet()));

        checkAuthorizationNotificationEvents(eventsResult.get(SyncTrxStatus.AUTHORIZED));
        checkAuthorizationNotificationRejectedEvents(eventsResult.get(SyncTrxStatus.REJECTED));
        checkConfirmNotificationEvents(eventsResult.get(SyncTrxStatus.REWARDED));
    }

    private void checkAuthorizationNotificationEvents(Set<TransactionInProgress> authorizationNotificationDTOS) {
        assertEquals(expectedAuthorizationNotificationEvents.size(), authorizationNotificationDTOS.size());
        assertEquals(
                sortAuthorizationEvents(expectedAuthorizationNotificationEvents),
                sortAuthorizationEvents(authorizationNotificationDTOS)
        );
    }

    private void checkAuthorizationNotificationRejectedEvents(Set<TransactionInProgress> authorizationNotificationDTOS) {
        assertEquals(expectedAuthorizationNotificationRejectedEvents.size(), authorizationNotificationDTOS.size());
        assertEquals(
                sortAuthorizationEvents(expectedAuthorizationNotificationRejectedEvents),
                sortAuthorizationEvents(authorizationNotificationDTOS)
        );
    }

    private void checkConfirmNotificationEvents(Set<TransactionInProgress> authorizationNotificationDTOS) {
        assertEquals(expectedConfirmNotificationEvents.size(), authorizationNotificationDTOS.size());
        assertEquals(
                sortConfirmEvents(expectedConfirmNotificationEvents),
                sortConfirmEvents(authorizationNotificationDTOS)
        );
    }

    private void checkErrorNotificationEvents() {
        int expectedNotificationEvents = expectedAuthorizationNotificationRejectedEvents.size();
        Map<String, TransactionInProgress> trxId2AuthEvent = expectedAuthorizationNotificationEvents.stream()
                .collect(Collectors.toMap(TransactionInProgress::getId, Function.identity()));
        List<ConsumerRecord<String,String>> consumerRecords = consumeMessages(topicErrors, expectedNotificationEvents,15000);
        assertEquals(expectedNotificationEvents,consumerRecords.size());

        Set<TransactionInProgress> eventsResult = consumerRecords.stream()
                .map(r -> {
                    TransactionInProgress out = TestUtils.jsonDeserializer(r.value(), TransactionInProgress.class);
                    assertEquals(out.getUserId(), r.key());
                    checkAuthorizationDateTime(trxId2AuthEvent, out);
                    checkErrorMessageHeaders(topicConfirmNotification, null, r, "TODO", "TODO", out.getUserId());

                    return out;
                })
                .collect(Collectors.toSet());

        assertEquals(
                sortAuthorizationEvents(expectedAuthorizationNotificationEvents),
                sortAuthorizationEvents(eventsResult)
        );
    }

    private void checkAuthorizationDateTime(Map<String, TransactionInProgress> trxId2AuthEvent, TransactionInProgress out) {
        TransactionInProgress expectedEvent = trxId2AuthEvent.get(out.getId());
        Assertions.assertNotNull(expectedEvent);
        Duration diffAuthDateTime = Duration.between(out.getAuthDate(),
                expectedEvent.getAuthDate());
        Assertions.assertTrue(diffAuthDateTime
                .compareTo(Duration.ofSeconds(throttlingSeconds)) >= 0 ||
                out.getAuthDate().equals(expectedEvent.getAuthDate()));
        Assertions.assertTrue(diffAuthDateTime
                .compareTo(Duration.ofSeconds(10L * throttlingSeconds)) < 0);
        out.setAuthDate(expectedEvent.getAuthDate());
    }

    @NotNull
    private List<TransactionInProgress> sortAuthorizationEvents(Set<TransactionInProgress> list) {
        return list.stream()
                .sorted(Comparator.comparing(TransactionInProgress::getId))
                .toList();
    }

    @NotNull
    private List<TransactionInProgress> sortConfirmEvents(Set<TransactionInProgress> list) {
        return list.stream()
                .sorted(Comparator.comparing(TransactionInProgress::getMerchantFiscalCode))
                .toList();
    }

}
