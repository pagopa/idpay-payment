package it.gov.pagopa.payment.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.payment.BaseIntegrationTest;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.event.trx.dto.TransactionOutcomeDTO;
import it.gov.pagopa.payment.connector.event.trx.dto.mapper.TransactionInProgress2TransactionOutcomeDTOMapper;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
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
import org.mockito.Mockito;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
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

    private final Set<TransactionOutcomeDTO> expectedAuthorizationNotificationEvents = Collections.synchronizedSet(new HashSet<>());
    private final Set<TransactionOutcomeDTO> expectedAuthorizationNotificationRejectedEvents = Collections.synchronizedSet(new HashSet<>());
    private final Set<TransactionOutcomeDTO> expectedConfirmNotificationEvents= Collections.synchronizedSet(new HashSet<>());
    private final Set<TransactionOutcomeDTO> expectedErrors = Collections.synchronizedSet(new HashSet<>());

    @Autowired
    private RewardRuleRepository rewardRuleRepository;
    @Autowired
    private TransactionInProgressRepository transactionInProgressRepository;

    @Autowired
    private TransactionInProgress2TransactionResponseMapper transactionResponseMapper;
    @Autowired
    private TransactionInProgress2SyncTrxStatusMapper transactionInProgress2SyncTrxStatusMapper;
    @Autowired
    private TransactionInProgress2TransactionOutcomeDTOMapper transactionInProgress2TransactionOutcomeDTOMapper;

    @SpyBean
    private TransactionNotifierService transactionNotifierServiceSpy;

    @Value("${app.qrCode.throttlingSeconds}")
    private int throttlingSeconds;

    @Test
    void test() {
        int N = Math.max(useCases.size(), 50);

        rewardRuleRepository.save(RewardRule.builder().id(INITIATIVEID).build());

        configureMocks();

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
        checkErrorNotificationEvents();
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

    public static final String IDTRXISSUERPREFIX_AUTHNOTNOTIFIEDDUETOFALSE = "AUTHNOTNOTIFIEDDUETOFALSE";
    public static final String IDTRXISSUERPREFIX_AUTHNOTNOTIFIEDDUETOEXCEPTION = "AUTHNOTNOTIFIEDDUETOEXCEPTION";
    public static final String IDTRXISSUERPREFIX_CONFIRMNOTNOTIFIEDDUETOFALSE = "CONFIRMNOTNOTIFIEDDUETOFALSE";
    public static final String IDTRXISSUERPREFIX_CONFIRMNOTNOTIFIEDDUETOEXCEPTION = "CONFIRMNOTNOTIFIEDDUETOEXCEPTION";


    protected void configureMocks(){
        Mockito.doReturn(false).when(transactionNotifierServiceSpy)
                .notify(Mockito.argThat(arg->
                        (arg.getIdTrxIssuer().startsWith(IDTRXISSUERPREFIX_AUTHNOTNOTIFIEDDUETOFALSE) &&
                                SyncTrxStatus.AUTHORIZED.equals(arg.getStatus()))
                        ||
                        (arg.getIdTrxIssuer().startsWith(IDTRXISSUERPREFIX_CONFIRMNOTNOTIFIEDDUETOFALSE) &&
                                SyncTrxStatus.REWARDED.equals(arg.getStatus()))
                        ),
                        Mockito.any());

        Mockito.doThrow(new RuntimeException("DUMMYEXCEPTION")).when(transactionNotifierServiceSpy)
                .notify(Mockito.argThat(arg->
                        (arg.getIdTrxIssuer().startsWith(IDTRXISSUERPREFIX_AUTHNOTNOTIFIEDDUETOEXCEPTION) &&
                                SyncTrxStatus.AUTHORIZED.equals(arg.getStatus()))
                        ||
                        (arg.getIdTrxIssuer().startsWith(IDTRXISSUERPREFIX_CONFIRMNOTNOTIFIEDDUETOEXCEPTION) &&
                                SyncTrxStatus.REWARDED.equals(arg.getStatus()))
                        ),
                        Mockito.any());
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

        // useCase 6: an error occurred when publishing authorized event, returned false
        useCases.add(i-> configureAuthEventNotPublishedDueToError(i,IDTRXISSUERPREFIX_AUTHNOTNOTIFIEDDUETOFALSE));

        // useCase 7: an error occurred when publishing authorized event, throwing error
        useCases.add(i-> configureAuthEventNotPublishedDueToError(i,IDTRXISSUERPREFIX_AUTHNOTNOTIFIEDDUETOEXCEPTION));

        // useCase 8: an error occurred when publishing confirmed event, returned false
        useCases.add(i-> configureConfirmEventNotPublishedDueToError(i,IDTRXISSUERPREFIX_CONFIRMNOTNOTIFIEDDUETOFALSE));

        // useCase 9: an error occurred when publishing confirmed event, throwing error
        useCases.add(i-> configureConfirmEventNotPublishedDueToError(i,IDTRXISSUERPREFIX_CONFIRMNOTNOTIFIEDDUETOEXCEPTION));

        // useCase 10: merchant not related to the initiative
        useCases.add(i -> {
            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId(INITIATIVEID);

            extractResponse(createTrx(trxRequest, "DUMMYMERCHANTID", ACQUIRERID, IDTRXACQUIRER), HttpStatus.FORBIDDEN, null);
        });

        useCases.addAll(getExtraUseCases());
    }

    private TransactionInProgress configureAuthEventNotPublishedDueToError(Integer i, String idTrxIssuerPrefix) throws Exception {
        TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
        trxRequest.setInitiativeId(INITIATIVEID);
        trxRequest.setIdTrxIssuer("%s_%s".formatted(idTrxIssuerPrefix, trxRequest.getIdTrxIssuer()));

        TransactionResponse trxCreated = createTrxSuccess(trxRequest);

        extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);

        AuthPaymentDTO authResult = extractResponse(authTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
        assertEquals(SyncTrxStatus.AUTHORIZED, authResult.getStatus());

        TransactionInProgress authStored = checkIfStored(trxCreated.getId());
        expectedErrors.add(transactionInProgress2TransactionOutcomeDTOMapper.apply(authStored));

        return authStored;
    }

    private void configureConfirmEventNotPublishedDueToError(Integer i, String idTrxIssuerPrefix) throws Exception {
        TransactionInProgress trx = configureAuthEventNotPublishedDueToError(i, idTrxIssuerPrefix);

        TransactionResponse trxResponse = transactionResponseMapper.apply(trx);

        addExpectedAuthorizationEvent(trxResponse);

        AuthPaymentDTO confirmResult = extractResponse(confirmPayment(trxResponse, MERCHANTID, ACQUIRERID), HttpStatus.OK, AuthPaymentDTO.class);
        assertEquals(SyncTrxStatus.REWARDED, confirmResult.getStatus());

        trx.setStatus(SyncTrxStatus.REWARDED);
    }

    private void addExpectedAuthorizationEvent(TransactionResponse trx) {
        TransactionInProgress trxAuth = checkIfStored(trx.getId());
        expectedAuthorizationNotificationEvents.add(transactionInProgress2TransactionOutcomeDTOMapper.apply(trxAuth));
    }

    private void addExpectedAuthorizationEventRejected(TransactionResponse trx) {
        TransactionInProgress trxRejected = checkIfStored(trx.getId());
        expectedAuthorizationNotificationRejectedEvents.add(transactionInProgress2TransactionOutcomeDTOMapper.apply(trxRejected));
    }

    private void addExpectedConfirmEvent(TransactionResponse trx){
        TransactionInProgress transactionConfirmed = checkIfStored(trx.getId());
        transactionConfirmed.setStatus(SyncTrxStatus.REWARDED);
        expectedConfirmNotificationEvents.add(transactionInProgress2TransactionOutcomeDTOMapper.apply(transactionConfirmed));
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

        Map<SyncTrxStatus, Set<TransactionOutcomeDTO>> eventsResult = consumerRecords.stream()
                .map(r -> {
                    TransactionOutcomeDTO out = TestUtils.jsonDeserializer(r.value(), TransactionOutcomeDTO.class);
                    if (out.getStatus().equals(SyncTrxStatus.REWARDED)) {
                        assertEquals(out.getMerchantId(), r.key());
                    } else {
                        assertEquals(out.getUserId(), r.key());
                    }

                    return out;
                })
                .collect(Collectors.groupingBy(TransactionOutcomeDTO::getStatus, Collectors.toSet()));

        checkAuthorizationNotificationEvents(eventsResult.get(SyncTrxStatus.AUTHORIZED));
        checkAuthorizationNotificationRejectedEvents(eventsResult.get(SyncTrxStatus.REJECTED));
        checkConfirmNotificationEvents(eventsResult.get(SyncTrxStatus.REWARDED));
    }

    private void checkAuthorizationNotificationEvents(Set<TransactionOutcomeDTO> authorizationNotificationDTOS) {
        assertEquals(expectedAuthorizationNotificationEvents.size(), authorizationNotificationDTOS.size());
        assertEquals(
                sortAuthorizationEvents(expectedAuthorizationNotificationEvents),
                sortAuthorizationEvents(authorizationNotificationDTOS)
        );
    }

    private void checkAuthorizationNotificationRejectedEvents(Set<TransactionOutcomeDTO> authorizationNotificationDTOS) {
        assertEquals(expectedAuthorizationNotificationRejectedEvents.size(), authorizationNotificationDTOS.size());
        assertEquals(
                sortAuthorizationEvents(expectedAuthorizationNotificationRejectedEvents),
                sortAuthorizationEvents(authorizationNotificationDTOS)
        );
    }

    private void checkConfirmNotificationEvents(Set<TransactionOutcomeDTO> confirmNotificationDTOS) {
        assertEquals(expectedConfirmNotificationEvents.size(), confirmNotificationDTOS.size());
        assertEquals(
                sortConfirmEvents(expectedConfirmNotificationEvents),
                sortConfirmEvents(confirmNotificationDTOS)
        );
    }

    private void checkErrorNotificationEvents() {
        int expectedNotificationEvents = expectedErrors.size();

        List<ConsumerRecord<String,String>> consumerRecords = consumeMessages(topicErrors, expectedNotificationEvents,15000);
        assertEquals(expectedNotificationEvents,consumerRecords.size());

        Set<TransactionOutcomeDTO> eventsResult = consumerRecords.stream()
                .map(r -> {
                    TransactionOutcomeDTO out = TestUtils.jsonDeserializer(r.value(), TransactionOutcomeDTO.class);
                    String expectedKey;
                    String expectedErrorDescription;

                    if(SyncTrxStatus.AUTHORIZED.equals(out.getStatus())){
                        expectedKey=out.getUserId();
                        expectedErrorDescription = "[QR_CODE_AUTHORIZE_TRANSACTION] An error occurred while publishing the Authorization Payment result: trxId %s - userId %s".formatted(out.getId(), out.getUserId());
                    } else {
                        expectedKey= out.getMerchantId();
                        expectedErrorDescription= "[QR_CODE_CONFIRM_PAYMENT] An error occurred while publishing the confirmation Payment result: trxId %s - merchantId %s - acquirerId %s".formatted(out.getId(), out.getMerchantId(), out.getAcquirerId());
                    }

                    checkErrorMessageHeaders(topicConfirmNotification, null, r, expectedErrorDescription, null, expectedKey, false, false);

                    return out;
                })
                .collect(Collectors.toSet());
        assertEquals(
                sortAuthorizationEvents(expectedErrors.stream().filter(i -> SyncTrxStatus.AUTHORIZED.equals(i.getStatus()) && !i.getIdTrxIssuer().contains(IDTRXISSUERPREFIX_CONFIRMNOTNOTIFIEDDUETOFALSE) && !i.getIdTrxIssuer().contains(IDTRXISSUERPREFIX_CONFIRMNOTNOTIFIEDDUETOEXCEPTION)).collect(Collectors.toSet())),
                sortAuthorizationEvents(eventsResult.stream().filter(i -> SyncTrxStatus.AUTHORIZED.equals(i.getStatus())).collect(Collectors.toSet()))
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
    private List<TransactionOutcomeDTO> sortAuthorizationEvents(Set<TransactionOutcomeDTO> list) {
        return list.stream()
                .sorted(Comparator.comparing(TransactionOutcomeDTO::getId))
                .toList();
    }

    @NotNull
    private List<TransactionOutcomeDTO> sortConfirmEvents(Set<TransactionOutcomeDTO> list) {
        return list.stream()
                .sorted(Comparator.comparing(TransactionOutcomeDTO::getMerchantFiscalCode))
                .toList();
    }

}
