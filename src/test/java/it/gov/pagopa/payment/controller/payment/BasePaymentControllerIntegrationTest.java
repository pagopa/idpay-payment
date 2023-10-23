package it.gov.pagopa.payment.controller.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.BaseIntegrationTest;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.event.trx.dto.TransactionOutcomeDTO;
import it.gov.pagopa.payment.connector.event.trx.dto.mapper.TransactionInProgress2TransactionOutcomeDTOMapper;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2SyncTrxStatusMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.InitiativeRewardType;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.InitiativeConfig;
import it.gov.pagopa.payment.model.RewardRule;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.TransactionCreationRequestFaker;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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

@TestPropertySource(
        properties = {
                "logging.level.it.gov.pagopa.payment=WARN",
                "logging.level.it.gov.pagopa.common=WARN",
                "logging.level.it.gov.pagopa.common.web.exception.ErrorManager=OFF",
                "logging.level.AUDIT=OFF",
                "app.qrCode.throttlingSeconds=2"
        })
abstract class BasePaymentControllerIntegrationTest extends BaseIntegrationTest {

    public static final String INITIATIVEID = "INITIATIVEID";
    public static final String INITIATIVEID_NOT_STARTED = INITIATIVEID + "1";
    public static final String USERID = "USERID";
    public static final String MERCHANTID = "MERCHANTID";
    public static final String ACQUIRERID = "ACQUIRERID";
    public static final String IDTRXISSUER = "IDTRXISSUER";
    public static final LocalDate TODAY = LocalDate.now();

    private static final int parallelism = 8;
    private static final ExecutorService executor = Executors.newFixedThreadPool(parallelism);

    private final List<FailableConsumer<Integer, Exception>> useCases = new ArrayList<>();

    private final Set<TransactionOutcomeDTO> expectedAuthorizationNotificationEvents = Collections.synchronizedSet(new HashSet<>());
    private final Set<TransactionOutcomeDTO> expectedAuthorizationNotificationRejectedEvents = Collections.synchronizedSet(new HashSet<>());
    private final Set<TransactionOutcomeDTO> expectedConfirmNotificationEvents = Collections.synchronizedSet(new HashSet<>());
    private final Set<TransactionOutcomeDTO> expectedCancelledNotificationEvents = Collections.synchronizedSet(new HashSet<>());
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
    @Autowired
    private TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper;

    @SpyBean
    private TransactionNotifierService transactionNotifierServiceSpy;

    @Value("${app.qrCode.throttlingSeconds}")
    private int throttlingSeconds;

    @Test
    void test() throws Exception {
        int N = Math.max(useCases.size(), 50);

        rewardRuleRepository.save(RewardRule.builder().id(INITIATIVEID)
                .initiativeConfig(InitiativeConfig.builder()
                        .initiativeId(INITIATIVEID)
                        .initiativeRewardType(InitiativeRewardType.DISCOUNT)
                        .startDate(TODAY.minusDays(1))
                        .endDate(TODAY.plusDays(1))
                        .build())
                .build());

        // rule for useCase 20
        rewardRuleRepository.save(RewardRule.builder().id(INITIATIVEID_NOT_STARTED)
                .initiativeConfig(InitiativeConfig.builder()
                        .initiativeId(INITIATIVEID_NOT_STARTED)
                        .initiativeRewardType(InitiativeRewardType.DISCOUNT)
                        .startDate(TODAY.plusDays(1))
                        .endDate(TODAY.plusDays(1))
                        .build())
                .build());

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
                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                } else if (e.getCause() instanceof AssertionFailedError assertionFailedError) {
                    throw assertionFailedError;
                }
                Assertions.fail(e);
            }
        }

        checkNotificationEventsOnTransactionQueue();

        //verifying error event notification
        checkErrorNotificationEvents();

        checkForceExpiration();
    }

    /**
     * Controller's channel
     */
    protected abstract String getChannel();

    /**
     * Invoke create transaction API acting as <i>merchantId</i>
     */
    protected abstract MvcResult createTrx(TransactionCreationRequest trxRequest, String merchantId, String acquirerId, String idTrxIssuer) throws Exception;

    /**
     * Invoke pre-authorize transaction API to relate <i>userId</i> to the transaction created by <i>merchantId</i>
     */
    protected abstract MvcResult preAuthTrx(TransactionResponse trx, String userid, String merchantId) throws Exception;

    /**
     * Invoke pre-authorize transaction API to authorize the transaction acting as <i>userId</i> to the transaction created by <i>merchantId</i>
     */
    protected abstract MvcResult authTrx(TransactionResponse trx, String userid, String merchantId) throws Exception;

    protected abstract MvcResult unrelateTrx(TransactionResponse trx, String userId) throws Exception;

    /**
     * Invoke confirm payment API acting as <i>merchantId</i>
     */
    protected abstract MvcResult confirmPayment(TransactionResponse trx, String merchantId, String acquirerId) throws Exception;

    /**
     * Invoke cancel payment API acting as <i>merchantId</i>
     */
    protected abstract MvcResult cancelTrx(TransactionResponse trx, String merchantId, String acquirerId) throws Exception;

    /**
     * Invoke getStatusTransaction API acting as <i>merchantId</i>
     */
    protected abstract MvcResult getStatusTransaction(String transactionId, String merchantId, String acquirerId) throws Exception;

    /**
     * Force auth transaction expiration
     */
    protected abstract MvcResult forceAuthExpiration(String initiativeId) throws Exception;

    /**
     * Force confirm transaction expiration
     */
    protected abstract MvcResult forceConfirmExpiration(String initiativeId) throws Exception;

    /**
     * Override in order to add specific use cases
     */
    protected List<FailableConsumer<Integer, Exception>> getExtraUseCases() {
        return Collections.emptyList();
    }

    private TransactionResponse createTrxSuccess(TransactionCreationRequest trxRequest) throws Exception {
        TransactionResponse trxCreated = extractResponse(createTrx(trxRequest, MERCHANTID, ACQUIRERID, IDTRXISSUER), HttpStatus.CREATED, TransactionResponse.class);
        assertEquals(SyncTrxStatus.CREATED, trxCreated.getStatus());
        checkTransactionStored(trxCreated);
        assertTrxCreatedData(trxRequest, trxCreated);
        return trxCreated;
    }

    private void checkTransactionStored(TransactionResponse trxCreated) throws Exception {
        TransactionInProgress stored = checkIfStored(trxCreated.getId());
        // Authorized merchant
        SyncTrxStatusDTO syncTrxStatusResult = extractResponse(getStatusTransaction(trxCreated.getId(), trxCreated.getMerchantId(), trxCreated.getAcquirerId()), HttpStatus.OK, SyncTrxStatusDTO.class);
        assertEquals(transactionInProgress2SyncTrxStatusMapper.transactionInProgressMapper(stored), syncTrxStatusResult);
        //Unauthorized operator
        extractResponse(getStatusTransaction(trxCreated.getId(), "DUMMYMERCHANTID", trxCreated.getAcquirerId()), HttpStatus.NOT_FOUND, null);

        assertEquals(getChannel(), stored.getChannel());
        trxCreated.setTrxDate(OffsetDateTime.parse(
                trxCreated.getTrxDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx"))));
        assertEquals(trxCreated, transactionResponseMapper.apply(stored));
    }

    private void checkTransactionStored(AuthPaymentDTO trx, String expectedUserId) {
        TransactionInProgress stored = checkIfStored(trx.getId());

        assertEquals(trx.getId(), stored.getId());
        assertEquals(trx.getTrxCode(), stored.getTrxCode());
        assertEquals(trx.getInitiativeId(), stored.getInitiativeId());
        assertEquals(trx.getStatus(), stored.getStatus());
        assertEquals(trx.getReward(), stored.getReward());
        assertNull(trx.getRejectionReasons());

        assertEquals(expectedUserId, stored.getUserId());
    }

    private TransactionInProgress checkIfStored(String trxId) {
        TransactionInProgress stored = transactionInProgressRepository.findById(trxId).orElse(null);
        Assertions.assertNotNull(stored);
        return stored;
    }

    public static final String IDTRXACQUIRERPREFIX_AUTHNOTNOTIFIEDDUETOFALSE = "AUTHNOTNOTIFIEDDUETOFALSE";
    public static final String IDTRXACQUIRERPREFIX_AUTHNOTNOTIFIEDDUETOEXCEPTION = "AUTHNOTNOTIFIEDDUETOEXCEPTION";
    public static final String IDTRXACQUIRERPREFIX_CONFIRMNOTNOTIFIEDDUETOFALSE = "CONFIRMNOTNOTIFIEDDUETOFALSE";
    public static final String IDTRXACQUIRERPREFIX_CONFIRMNOTNOTIFIEDDUETOEXCEPTION = "CONFIRMNOTNOTIFIEDDUETOEXCEPTION";
    public static final String IDTRXACQUIRERPREFIX_CANCELLEDNOTNOTIFIEDDUETOFALSE = "CANCELLEDNOTNOTIFIEDDUETOFALSE";
    public static final String IDTRXACQUIRERPREFIX_CANCELLEDNOTNOTIFIEDDUETOEXCEPTION = "CANCELLEDNOTNOTIFIEDDUETOEXCEPTION";


    protected void configureMocks() {
        Mockito.doReturn(false).when(transactionNotifierServiceSpy)
                .notify(Mockito.argThat(arg ->
                                (arg.getIdTrxAcquirer().startsWith(IDTRXACQUIRERPREFIX_AUTHNOTNOTIFIEDDUETOFALSE) &&
                                        SyncTrxStatus.AUTHORIZED.equals(arg.getStatus()))
                                        ||
                                        (arg.getIdTrxAcquirer().startsWith(IDTRXACQUIRERPREFIX_CONFIRMNOTNOTIFIEDDUETOFALSE) &&
                                                SyncTrxStatus.REWARDED.equals(arg.getStatus()))
                                        ||
                                        (arg.getIdTrxAcquirer().startsWith(IDTRXACQUIRERPREFIX_CANCELLEDNOTNOTIFIEDDUETOFALSE) &&
                                                SyncTrxStatus.CANCELLED.equals(arg.getStatus()))
                        ),
                        Mockito.any());

        Mockito.doThrow(new RuntimeException("DUMMYEXCEPTION")).when(transactionNotifierServiceSpy)
                .notify(Mockito.argThat(arg ->
                                (arg.getIdTrxAcquirer().startsWith(IDTRXACQUIRERPREFIX_AUTHNOTNOTIFIEDDUETOEXCEPTION) &&
                                        SyncTrxStatus.AUTHORIZED.equals(arg.getStatus()))
                                        ||
                                        (arg.getIdTrxAcquirer().startsWith(IDTRXACQUIRERPREFIX_CONFIRMNOTNOTIFIEDDUETOEXCEPTION) &&
                                                SyncTrxStatus.REWARDED.equals(arg.getStatus()))
                                        ||
                                        (arg.getIdTrxAcquirer().startsWith(IDTRXACQUIRERPREFIX_CANCELLEDNOTNOTIFIEDDUETOEXCEPTION) &&
                                                SyncTrxStatus.CANCELLED.equals(arg.getStatus()))
                        ),
                        Mockito.any());
    }


    {
        // useCase 0: initiative not existent
        useCases.add(i -> {
            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId("DUMMYINITIATIVEID");

            extractResponse(createTrx(trxRequest, MERCHANTID, ACQUIRERID, IDTRXISSUER), HttpStatus.NOT_FOUND, null);

            // Other APIs cannot be invoked because we have not a valid trxId
            TransactionResponse dummyTrx = TransactionResponse.builder().id("DUMMYTRXID").trxCode("dummytrxcode").trxDate(OffsetDateTime.now()).build();
            extractResponse(preAuthTrx(dummyTrx, USERID, MERCHANTID), HttpStatus.NOT_FOUND, null);
            extractResponse(authTrx(dummyTrx, USERID, MERCHANTID), HttpStatus.NOT_FOUND, null);
            extractResponse(confirmPayment(dummyTrx, MERCHANTID, ACQUIRERID), HttpStatus.NOT_FOUND, null);
        });

        // useCase 1: userId not onboarded
        useCases.add(i -> {
            String userIdNotOnboarded = "DUMMYUSERIDNOTONBOARDED";

            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId(INITIATIVEID);

            // Creating transaction
            TransactionResponse trxCreated = createTrxSuccess(trxRequest);

            // Cannot relate user because not onboarded
            extractResponse(preAuthTrx(trxCreated, userIdNotOnboarded, MERCHANTID), HttpStatus.FORBIDDEN, null);

            // Other APIs will fail because status not expected
            extractResponse(authTrx(trxCreated, userIdNotOnboarded, MERCHANTID), HttpStatus.FORBIDDEN, null);
            extractResponse(confirmPayment(trxCreated, MERCHANTID, ACQUIRERID), HttpStatus.BAD_REQUEST, null);
        });

        // useCase 2: trx rejected
        useCases.add(i -> {
            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId(INITIATIVEID);
            trxRequest.setMcc("NOTALLOWEDMCC");

            // Creating transaction
            TransactionResponse trxCreated = createTrxSuccess(trxRequest);

            // Relating to user
            extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), HttpStatus.FORBIDDEN, null);

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
            checkTransactionStored(preAuthResult, USERID);

            // Authorizing transaction, but obtaining rejection
            updateStoredTransaction(preAuthResult.getId(), t -> t.setMcc("NOTALLOWEDMCC"));
            extractResponse(authTrx(trxCreated, USERID, MERCHANTID), HttpStatus.FORBIDDEN, AuthPaymentDTO.class);

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
            // Relating to user resubmission
            AuthPaymentDTO preAuthResultResubmitted = extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
            assertEquals(preAuthResult, preAuthResultResubmitted);
            checkTransactionStored(preAuthResult, USERID);
            // Only the right userId could resubmit preview
            extractResponse(preAuthTrx(trxCreated, "DUMMYUSERID", MERCHANTID), HttpStatus.FORBIDDEN, null);

            // Cannot invoke other APIs if not authorizing first
            extractResponse(confirmPayment(trxCreated, MERCHANTID, ACQUIRERID), HttpStatus.BAD_REQUEST, null);
            updateStoredTransaction(trxCreated.getId(), t -> {
                // resetting throttling data in order to assert auth data
                t.setElaborationDateTime(null);
            });

            // Only the right userId could authorize its transaction
            extractResponse(authTrx(trxCreated, "DUMMYUSERID", MERCHANTID), HttpStatus.FORBIDDEN, null);

            waitThrottlingTime();

            // Authorizing transaction
            AuthPaymentDTO authResult = extractResponse(authTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);

            assertEquals(SyncTrxStatus.AUTHORIZED, authResult.getStatus());
            assertAuthData(authResult, true);
            // Cannot invoke preAuth after authorization
            extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), HttpStatus.FORBIDDEN, null);
            // Authorizing transaction resubmission after throttling time
            waitThrottlingTime();

            //setpayload authResult
            addExpectedAuthorizationEvent(trxCreated);

            updateStoredTransaction(authResult.getId(), t -> t.setCorrelationId("ALREADY_AUTHORED"));
            extractResponse(authTrx(trxCreated, USERID, MERCHANTID), HttpStatus.FORBIDDEN, AuthPaymentDTO.class);

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

            //cannot cancel after confirm
            extractResponse(cancelTrx(trxCreated, MERCHANTID, ACQUIRERID), HttpStatus.NOT_FOUND, null);
        });

        // useCase 6: an error occurred when publishing authorized event, returned false
        useCases.add(i -> configureAuthEventNotPublishedDueToError(i, IDTRXACQUIRERPREFIX_AUTHNOTNOTIFIEDDUETOFALSE));
        // useCase 7: an error occurred when publishing authorized event, throwing error
        useCases.add(i -> configureAuthEventNotPublishedDueToError(i, IDTRXACQUIRERPREFIX_AUTHNOTNOTIFIEDDUETOEXCEPTION));

        // useCase 8: an error occurred when publishing confirmed event, returned false
        useCases.add(i -> configureConfirmEventNotPublishedDueToError(i, IDTRXACQUIRERPREFIX_CONFIRMNOTNOTIFIEDDUETOFALSE));
        // useCase 9: an error occurred when publishing confirmed event, throwing error
        useCases.add(i -> configureConfirmEventNotPublishedDueToError(i, IDTRXACQUIRERPREFIX_CONFIRMNOTNOTIFIEDDUETOEXCEPTION));

        // useCase 10: merchant not related to the initiative
        useCases.add(i -> {
            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId(INITIATIVEID);

            extractResponse(createTrx(trxRequest, "DUMMYMERCHANTID", ACQUIRERID, IDTRXISSUER), HttpStatus.NOT_FOUND, null);
        });

        //useCase 11: obtain unexpected http code from ms idpay-merchant
        useCases.add(i -> {
            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId(INITIATIVEID);

            extractResponse(createTrx(trxRequest, "ERRORMERCHANTID", ACQUIRERID, IDTRXISSUER), HttpStatus.INTERNAL_SERVER_ERROR, null);
        });

        // useCase 12: trx cancelled after create
        useCases.add(i -> {
            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId(INITIATIVEID);

            // Creating transaction
            TransactionResponse trxCreated = createTrxSuccess(trxRequest);

            extractResponse(cancelTrx(trxCreated, MERCHANTID, ACQUIRERID), HttpStatus.OK, null);

            Assertions.assertFalse(transactionInProgressRepository.existsById(trxCreated.getId()));

            extractResponse(cancelTrx(trxCreated, MERCHANTID, ACQUIRERID), HttpStatus.NOT_FOUND, null);
        });

        // useCase 13: trx cancelled after preAuth
        useCases.add(i -> {
            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId(INITIATIVEID);

            // Creating transaction
            TransactionResponse trxCreated = createTrxSuccess(trxRequest);

            // Relating to user
            AuthPaymentDTO preAuthResult = extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
            assertEquals(SyncTrxStatus.IDENTIFIED, preAuthResult.getStatus());
            checkTransactionStored(preAuthResult, USERID);

            extractResponse(cancelTrx(trxCreated, MERCHANTID, ACQUIRERID), HttpStatus.OK, null);

            Assertions.assertFalse(transactionInProgressRepository.existsById(trxCreated.getId()));

            extractResponse(cancelTrx(trxCreated, MERCHANTID, ACQUIRERID), HttpStatus.NOT_FOUND, null);
        });

        // useCase 14: trx cancelled after auth
        useCases.add(i -> {
            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId(INITIATIVEID);

            // Creating transaction
            TransactionResponse trxCreated = createTrxSuccess(trxRequest);

            changeTrxId2MatchCancelMatchedCondition(trxCreated);

            // Relating to user
            AuthPaymentDTO preAuthResult = extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
            assertEquals(SyncTrxStatus.IDENTIFIED, preAuthResult.getStatus());
            checkTransactionStored(preAuthResult, USERID);

            // Authorizing transaction, but obtaining rejection
            AuthPaymentDTO authResult = extractResponse(authTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
            assertEquals(SyncTrxStatus.AUTHORIZED, authResult.getStatus());
            assertAuthData(authResult, true);

            addExpectedAuthorizationEvent(trxCreated);

            addExpectedCancelledEvent(trxCreated);
            extractResponse(cancelTrx(trxCreated, MERCHANTID, ACQUIRERID), HttpStatus.OK, null);

            Assertions.assertFalse(transactionInProgressRepository.existsById(trxCreated.getId()));

            extractResponse(cancelTrx(trxCreated, MERCHANTID, ACQUIRERID), HttpStatus.NOT_FOUND, null);
        });

        // useCase 15: an error occurred when publishing confirmed event, returned false
        useCases.add(i -> configureCancelledEventNotPublishedDueToError(i, IDTRXACQUIRERPREFIX_CANCELLEDNOTNOTIFIEDDUETOFALSE));
        // useCase 16: an error occurred when publishing confirmed event, throwing error
        useCases.add(i -> configureCancelledEventNotPublishedDueToError(i, IDTRXACQUIRERPREFIX_CANCELLEDNOTNOTIFIEDDUETOEXCEPTION));
        //useCase 17: trx rejected budget exhausted
        useCases.add(i -> {
            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId(INITIATIVEID);
            trxRequest.setMcc("NOTALLOWEDMCC1");

            // Creating transaction
            TransactionResponse trxCreated = createTrxSuccess(trxRequest);

            // Relating to user
            extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), HttpStatus.FORBIDDEN, null);

            // Cannot invoke other APIs if REJECTED
            extractResponse(authTrx(trxCreated, USERID, MERCHANTID), HttpStatus.BAD_REQUEST, null);
            extractResponse(confirmPayment(trxCreated, MERCHANTID, ACQUIRERID), HttpStatus.BAD_REQUEST, null);
        });

        //useCase 18: user cancel payment instead of authorizing
        useCases.add(i -> {
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

        //useCase 19: merchant tries to create transaction with amount = 0
        useCases.add(i -> {
            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId(INITIATIVEID);
            trxRequest.setAmountCents(0L);

            extractResponse(createTrx(trxRequest, MERCHANTID, ACQUIRERID, IDTRXISSUER), HttpStatus.BAD_REQUEST, null);
        });

        //useCase 20: merchant tries to create transaction out of valid initiative period
        useCases.add(i -> {
            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId(INITIATIVEID_NOT_STARTED);

            // Creating transaction
            extractResponse(createTrx(trxRequest, MERCHANTID, ACQUIRERID, IDTRXISSUER), HttpStatus.FORBIDDEN, null);
        });

        useCases.addAll(getExtraUseCases());
    }

    private static void cleanDatesAndCheckUnrelatedTrx(TransactionInProgress preAuthTrx, TransactionInProgress unrelated) {
        Assertions.assertNotNull(preAuthTrx.getUpdateDate());
        preAuthTrx.setUpdateDate(null);
        Assertions.assertNotNull(unrelated.getUpdateDate());
        unrelated.setUpdateDate(null);

        Assertions.assertEquals(preAuthTrx, unrelated);
    }

    private void changeTrxId2MatchCancelMatchedCondition(TransactionResponse trxCreated) {
        // changing id in order to match cancel stub condition
        TransactionInProgress stored = transactionInProgressRepository.findById(trxCreated.getId()).orElse(null);
        Assertions.assertNotNull(stored);
        transactionInProgressRepository.delete(stored);
        stored.setId("CANCELLEDAFTERAUTHORIZATIONTRX" + stored.getId());
        stored.setCorrelationId(stored.getId());
        transactionInProgressRepository.save(stored);
        trxCreated.setId(stored.getId());
    }

    private Pair<TransactionInProgress, TransactionOutcomeDTO> configureAuthEventNotPublishedDueToError(Integer i, String idTrxAcquirerPrefix) throws Exception {
        TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
        trxRequest.setInitiativeId(INITIATIVEID);
        trxRequest.setIdTrxAcquirer("%s_%s".formatted(idTrxAcquirerPrefix, trxRequest.getIdTrxAcquirer()));

        TransactionResponse trxCreated = createTrxSuccess(trxRequest);

        extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);

        AuthPaymentDTO authResult = extractResponse(authTrx(trxCreated, USERID, MERCHANTID), HttpStatus.OK, AuthPaymentDTO.class);
        assertEquals(SyncTrxStatus.AUTHORIZED, authResult.getStatus());

        TransactionInProgress authStored = checkIfStored(trxCreated.getId());
        TransactionOutcomeDTO expectedNotification = transactionInProgress2TransactionOutcomeDTOMapper.apply(authStored);
        expectedErrors.add(expectedNotification);

        expectedNotification.setElaborationDateTime(TestUtils.truncateTimestamp(expectedNotification.getElaborationDateTime()));

        return Pair.of(authStored, expectedNotification);
    }

    private void configureConfirmEventNotPublishedDueToError(Integer i, String idTrxIssuerPrefix) throws Exception {
        Pair<TransactionInProgress, TransactionOutcomeDTO> trx = configureAuthEventNotPublishedDueToError(i, idTrxIssuerPrefix);

        TransactionResponse trxResponse = transactionResponseMapper.apply(trx.getKey());

        addExpectedAuthorizationEvent(trxResponse);

        AuthPaymentDTO confirmResult = extractResponse(confirmPayment(trxResponse, MERCHANTID, ACQUIRERID), HttpStatus.OK, AuthPaymentDTO.class);
        assertEquals(SyncTrxStatus.REWARDED, confirmResult.getStatus());

        // updating status on errorNotificationStored expected
        trx.getValue().setStatus(SyncTrxStatus.REWARDED);
        trx.getValue().setElaborationDateTime(TestUtils.truncateTimestamp(LocalDateTime.now()));
    }

    private void configureCancelledEventNotPublishedDueToError(Integer i, String idTrxIssuerPrefix) throws Exception {
        Pair<TransactionInProgress, TransactionOutcomeDTO> trx = configureAuthEventNotPublishedDueToError(i, idTrxIssuerPrefix);

        TransactionResponse trxResponse = transactionResponseMapper.apply(trx.getKey());
        addExpectedAuthorizationEvent(trxResponse);

        changeTrxId2MatchCancelMatchedCondition(trxResponse);

        extractResponse(cancelTrx(trxResponse, MERCHANTID, ACQUIRERID), HttpStatus.OK, null);

        // updating status on errorNotificationStored expected
        trx.getValue().setId(trxResponse.getId());
        trx.getValue().setCorrelationId(trxResponse.getId());
        trx.getValue().setStatus(SyncTrxStatus.CANCELLED);
        trx.getValue().setElaborationDateTime(TestUtils.truncateTimestamp(LocalDateTime.now()));
        trx.getValue().getRewards().values().forEach(r -> {
            r.setAccruedReward(r.getAccruedReward().negate());
            r.setProvidedReward(r.getProvidedReward().negate());
        });
    }

    private void addExpectedAuthorizationEvent(TransactionResponse trx) {
        TransactionInProgress trxAuth = checkIfStored(trx.getId());
        Assertions.assertNotEquals(Collections.emptyMap(), trxAuth.getRewards());
        expectedAuthorizationNotificationEvents.add(transactionInProgress2TransactionOutcomeDTOMapper.apply(trxAuth));
    }

    private void addExpectedAuthorizationEventRejected(TransactionResponse trx) {
        TransactionInProgress trxRejected = checkIfStored(trx.getId());
        expectedAuthorizationNotificationRejectedEvents.add(transactionInProgress2TransactionOutcomeDTOMapper.apply(trxRejected));
    }

    private void addExpectedConfirmEvent(TransactionResponse trx) {
        TransactionInProgress transactionConfirmed = checkIfStored(trx.getId());
        transactionConfirmed.setStatus(SyncTrxStatus.REWARDED);
        Assertions.assertNotEquals(Collections.emptyMap(), transactionConfirmed.getRewards());
        expectedConfirmNotificationEvents.add(transactionInProgress2TransactionOutcomeDTOMapper.apply(transactionConfirmed));
    }

    private void addExpectedCancelledEvent(TransactionResponse trx) {
        TransactionInProgress trxCancelled = checkIfStored(trx.getId());
        Assertions.assertNotEquals(Collections.emptyMap(), trxCancelled.getRewards());
        trxCancelled.setStatus(SyncTrxStatus.CANCELLED);
        trxCancelled.setReward(-trxCancelled.getReward());
        trxCancelled.getRewards().values().forEach(r -> {
            r.setAccruedReward(r.getAccruedReward().negate());
            r.setProvidedReward(r.getProvidedReward().negate());
        });
        trxCancelled.setElaborationDateTime(LocalDateTime.now());
        expectedCancelledNotificationEvents.add(transactionInProgress2TransactionOutcomeDTOMapper.apply(trxCancelled));
    }

    private void waitThrottlingTime() {
        wait(throttlingSeconds, TimeUnit.SECONDS);
    }

    protected void updateStoredTransaction(String trxId, Consumer<TransactionInProgress> updater) {
        TransactionInProgress stored = checkIfStored(trxId);
        updater.accept(stored);
        transactionInProgressRepository.save(stored);
    }

    private <T> T extractResponse(MvcResult response, HttpStatus expectedHttpStatusCode, Class<T> expectedBodyClass) {
        return TestUtils.assertResponse(response,expectedHttpStatusCode,expectedBodyClass);
    }

    private void checkNotificationEventsOnTransactionQueue() {
        int numExpectedNotification =
                expectedAuthorizationNotificationEvents.size() +
                        expectedConfirmNotificationEvents.size() +
                        expectedCancelledNotificationEvents.size();
        List<ConsumerRecord<String, String>> consumerRecords = kafkaTestUtilitiesService.consumeMessages(topicConfirmNotification, numExpectedNotification, 15000);

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
        checkConfirmNotificationEvents(eventsResult.get(SyncTrxStatus.REWARDED));
        checkCancelledNotificationEvents(eventsResult.get(SyncTrxStatus.CANCELLED));
    }

    private void checkAuthorizationNotificationEvents(Set<TransactionOutcomeDTO> authorizationNotificationDTOS) {
        assertNotifications(expectedAuthorizationNotificationEvents, authorizationNotificationDTOS);
    }

    private void checkAuthorizationNotificationRejectedEvents(Set<TransactionOutcomeDTO> authorizationNotificationDTOS) {
        assertNotifications(expectedAuthorizationNotificationRejectedEvents, authorizationNotificationDTOS);
    }

    private void checkConfirmNotificationEvents(Set<TransactionOutcomeDTO> confirmNotificationDTOS) {
        assertNotifications(expectedConfirmNotificationEvents, confirmNotificationDTOS);
    }

    private void checkCancelledNotificationEvents(Set<TransactionOutcomeDTO> cancelledNotificationDTOS) {
        assertNotifications(expectedCancelledNotificationEvents, cancelledNotificationDTOS);
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

    private void checkErrorNotificationEvents() {
        int expectedNotificationEvents = expectedErrors.size();

        List<ConsumerRecord<String, String>> consumerRecords = kafkaTestUtilitiesService.consumeMessages(topicErrors, expectedNotificationEvents, 15000);
        assertEquals(expectedNotificationEvents, consumerRecords.size());

        Set<TransactionOutcomeDTO> eventsResult = consumerRecords.stream()
                .map(r -> {
                    TransactionOutcomeDTO out = TestUtils.jsonDeserializer(r.value(), TransactionOutcomeDTO.class);
                    String expectedKey;
                    String expectedErrorDescription;

                    if (SyncTrxStatus.AUTHORIZED.equals(out.getStatus())) {
                        expectedKey = out.getUserId();
                        expectedErrorDescription = "[AUTHORIZE_TRANSACTION] An error occurred while publishing the Authorization Payment result: trxId %s - userId %s".formatted(out.getId(), out.getUserId());
                    } else if (SyncTrxStatus.REWARDED.equals(out.getStatus())) {
                        expectedKey = out.getMerchantId();
                        expectedErrorDescription = "[CONFIRM_PAYMENT] An error occurred while publishing the confirmation Payment result: trxId %s - merchantId %s - acquirerId %s".formatted(out.getId(), out.getMerchantId(), out.getAcquirerId());
                    } else {
                        expectedKey = out.getUserId();
                        expectedErrorDescription = "[CANCEL_TRANSACTION] An error occurred while publishing the cancellation authorized result: trxId %s - merchantId %s - acquirerId %s".formatted(out.getId(), out.getMerchantId(), out.getAcquirerId());
                    }

                    checkErrorMessageHeaders(topicConfirmNotification, null, r, expectedErrorDescription, r.value(), expectedKey, false, false);

                    if (out.getElaborationDateTime() != null) {
                        out.setElaborationDateTime(TestUtils.truncateTimestamp(out.getElaborationDateTime()));
                    }

                    return out;
                })
                .collect(Collectors.toSet());

        assertEquals(
                sortEvents(expectedErrors),
                sortEvents(eventsResult)
        );
    }

    private void checkAuthorizationDateTime(Map<String, TransactionInProgress> trxId2AuthEvent, TransactionInProgress out) {
        TransactionInProgress expectedEvent = trxId2AuthEvent.get(out.getId());
        Assertions.assertNotNull(expectedEvent);
        Duration diffAuthDateTime = Duration.between(out.getTrxChargeDate(),
                expectedEvent.getTrxChargeDate());
        Assertions.assertTrue(diffAuthDateTime
                .compareTo(Duration.ofSeconds(throttlingSeconds)) >= 0 ||
                out.getTrxChargeDate().equals(expectedEvent.getTrxChargeDate()));
        Assertions.assertTrue(diffAuthDateTime
                .compareTo(Duration.ofSeconds(10L * throttlingSeconds)) < 0);
        out.setTrxChargeDate(expectedEvent.getTrxChargeDate());
    }

    private void checkForceExpiration() throws Exception {
        Map<SyncTrxStatus, List<TransactionInProgress>> trxByStatus = transactionInProgressRepository.findAll().stream()
                .collect(Collectors.groupingBy(TransactionInProgress::getStatus));


        List<TransactionInProgress> expectedConfirmForced = trxByStatus.get(SyncTrxStatus.AUTHORIZED);
        List<TransactionInProgress> expectedAuthorizationForced = Arrays.stream(SyncTrxStatus.values())
                .filter(s -> !s.equals(SyncTrxStatus.AUTHORIZED))
                .map(trxByStatus::get)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList();

        Assertions.assertEquals(0L, extractResponse(forceAuthExpiration("DUMMYINITIATIVEID"), HttpStatus.OK, Long.class));
        Assertions.assertEquals(expectedAuthorizationForced.size(),
                extractResponse(forceAuthExpiration(INITIATIVEID), HttpStatus.OK, Long.class));

        Assertions.assertEquals(0L, extractResponse(forceConfirmExpiration("DUMMYINITIATIVEID"), HttpStatus.OK, Long.class));
        Assertions.assertEquals(
                expectedConfirmForced.size(),
                extractResponse(forceConfirmExpiration(INITIATIVEID), HttpStatus.OK, Long.class));

        expectedConfirmNotificationEvents.addAll(expectedConfirmForced.stream()
                .map(transactionInProgress2TransactionOutcomeDTOMapper)
                .peek(t -> t.setStatus(SyncTrxStatus.REWARDED))
                .toList());
        checkNotificationEventsOnTransactionQueue();
    }

    private List<TransactionOutcomeDTO> sortEvents(Set<TransactionOutcomeDTO> list) {
        return list.stream()
                .sorted(Comparator.comparing(TransactionOutcomeDTO::getId))
                .toList();
    }

    private void assertTrxCreatedData(TransactionCreationRequest trxRequest, TransactionResponse trxCreated) {
        assertCommonFields(trxRequest, trxCreated);

        TransactionInProgress stored = transactionInProgressRepository.findById(trxCreated.getId()).orElse(null);
        Assertions.assertNotNull(stored);
        assertCommonFields(trxCreated, stored, null, false);

        Assertions.assertNull(trxCreated.getSplitPayment());
        Assertions.assertNull(trxCreated.getResidualAmountCents());
    }

    private void assertPreAuthData(AuthPaymentDTO preAuthResult, boolean expectedRewarded) {
        TransactionInProgress stored = transactionInProgressRepository.findById(preAuthResult.getId()).orElse(null);
        Assertions.assertNotNull(stored);
        assertCommonFields(preAuthResult, stored, expectedRewarded);
    }

    private void assertAuthData(AuthPaymentDTO authResult, boolean expectedRewarded) {
        TransactionInProgress stored = transactionInProgressRepository.findById(authResult.getId()).orElse(null);
        Assertions.assertNotNull(stored);
        assertCommonFields(authResult, stored, expectedRewarded);
    }

    private void assertCommonFields(TransactionCreationRequest trxRequest, TransactionResponse trxResponse) {
        OffsetDateTime now = OffsetDateTime.now();
        Assertions.assertNotNull(trxResponse.getId());
        Assertions.assertNotNull(trxResponse.getTrxCode());
        Assertions.assertEquals(trxRequest.getInitiativeId(), trxResponse.getInitiativeId());
        Assertions.assertEquals(MERCHANTID, trxResponse.getMerchantId());
        Assertions.assertEquals(IDTRXISSUER, trxResponse.getIdTrxIssuer());
        Assertions.assertNotNull(trxResponse.getIdTrxAcquirer());
        Assertions.assertFalse(trxResponse.getTrxDate().isAfter(now.plusMinutes(1L)));
        Assertions.assertFalse(trxResponse.getTrxDate().isBefore(now.minusMinutes(1L)));
        Assertions.assertEquals(trxRequest.getAmountCents(), trxResponse.getAmountCents());
        Assertions.assertEquals(PaymentConstants.CURRENCY_EUR, trxResponse.getAmountCurrency());
        Assertions.assertEquals(trxRequest.getMcc(), trxResponse.getMcc());
        Assertions.assertEquals(ACQUIRERID, trxResponse.getAcquirerId());
        Assertions.assertEquals(SyncTrxStatus.CREATED, trxResponse.getStatus());
        Assertions.assertEquals("MERCHANTFISCALCODE0", trxResponse.getMerchantFiscalCode());
        Assertions.assertEquals("VAT0", trxResponse.getVat());
    }

    private void assertCommonFields(TransactionResponse trxResponse, TransactionInProgress trxStored, String userId, boolean expectedRewarded) {
        Assertions.assertEquals(trxResponse.getId(), trxStored.getId());
        Assertions.assertEquals(trxResponse.getTrxCode(), trxStored.getTrxCode());
        Assertions.assertEquals(trxResponse.getIdTrxAcquirer(), trxStored.getIdTrxAcquirer());
        Assertions.assertEquals(trxResponse.getTrxDate(), trxStored.getTrxDate());
        Assertions.assertEquals("00", trxStored.getOperationType());
        Assertions.assertEquals(OperationType.CHARGE, trxStored.getOperationTypeTranscoded());
        Assertions.assertEquals(trxResponse.getIdTrxIssuer(), trxStored.getIdTrxIssuer());
        Assertions.assertEquals(trxResponse.getId(), trxStored.getCorrelationId());
        Assertions.assertEquals(trxResponse.getAmountCents(), trxStored.getAmountCents());
        Assertions.assertEquals(CommonUtilities.centsToEuro(trxResponse.getAmountCents()), trxStored.getEffectiveAmount());
        Assertions.assertEquals(trxResponse.getAmountCurrency(), trxStored.getAmountCurrency());
        Assertions.assertEquals(trxResponse.getMcc(), trxStored.getMcc());
        Assertions.assertEquals(trxResponse.getAcquirerId(), trxStored.getAcquirerId());
        Assertions.assertEquals(trxResponse.getMerchantId(), trxStored.getMerchantId());
        Assertions.assertEquals(trxResponse.getMerchantFiscalCode(), trxStored.getMerchantFiscalCode());
        Assertions.assertEquals(trxResponse.getVat(), trxStored.getVat());
        Assertions.assertEquals(trxResponse.getInitiativeId(), trxStored.getInitiativeId());
        Assertions.assertEquals("INITIATIVENAME", trxStored.getInitiativeName());
        Assertions.assertEquals("BUSINESSNAME", trxStored.getBusinessName());
        Assertions.assertEquals(userId, trxStored.getUserId());
        Assertions.assertEquals(trxResponse.getStatus(), trxStored.getStatus());
        Assertions.assertEquals(getChannel(), trxStored.getChannel());
        Assertions.assertEquals(trxResponse.getQrcodePngUrl(), transactionInProgress2TransactionResponseMapper.generateTrxCodeImgUrl(trxStored.getTrxCode()));
        Assertions.assertEquals(trxResponse.getQrcodeTxtUrl(), transactionInProgress2TransactionResponseMapper.generateTrxCodeTxtUrl(trxStored.getTrxCode()));

        switch (trxStored.getStatus()) {
            case CREATED, IDENTIFIED -> {
                Assertions.assertNull(trxStored.getTrxChargeDate());
                Assertions.assertNull(trxStored.getElaborationDateTime());
                Assertions.assertNull(trxStored.getReward());
                Assertions.assertEquals(Collections.emptyList(), trxStored.getRejectionReasons());
                Assertions.assertEquals(Collections.emptyMap(), trxStored.getRewards());
            }
            case AUTHORIZED -> {
                Assertions.assertNotNull(trxStored.getTrxChargeDate());

                if (expectedRewarded) {
                    Assertions.assertNotNull(trxStored.getReward());
                    Assertions.assertEquals(Collections.emptyList(), trxStored.getRejectionReasons());
                    Assertions.assertNotNull(trxStored.getRewards());
                    Assertions.assertFalse(trxStored.getRewards().isEmpty());
                } else {
                    Assertions.assertEquals(0L, trxStored.getReward());
                    Assertions.assertNotEquals(Collections.emptyList(), trxStored.getRejectionReasons());
                    Assertions.assertEquals(Collections.emptyMap(), trxStored.getRewards());
                }

                Assertions.assertNull(trxStored.getElaborationDateTime());
            }
            default -> throw new IllegalStateException("Unexpected stored status:" + trxStored.getStatus());
        }
    }

    private void assertCommonFields(AuthPaymentDTO authPaymentDTO, TransactionInProgress trxStored, boolean expectedRewarded) {
        Assertions.assertEquals(authPaymentDTO.getId(), trxStored.getId());
        Assertions.assertEquals(authPaymentDTO.getTrxCode(), trxStored.getTrxCode());
        Assertions.assertNotNull(trxStored.getIdTrxAcquirer());
        Assertions.assertEquals(authPaymentDTO.getTrxDate(), trxStored.getTrxDate());
        Assertions.assertEquals("00", trxStored.getOperationType());
        Assertions.assertEquals(OperationType.CHARGE, trxStored.getOperationTypeTranscoded());
        Assertions.assertNotNull(trxStored.getIdTrxIssuer());
        Assertions.assertEquals(authPaymentDTO.getId(), trxStored.getCorrelationId());
        Assertions.assertEquals(authPaymentDTO.getAmountCents(), trxStored.getAmountCents());
        Assertions.assertEquals(CommonUtilities.centsToEuro(authPaymentDTO.getAmountCents()), trxStored.getEffectiveAmount());
        Assertions.assertEquals("EUR", trxStored.getAmountCurrency());
        Assertions.assertNotNull(trxStored.getMcc());
        Assertions.assertEquals(ACQUIRERID, trxStored.getAcquirerId());
        Assertions.assertEquals(MERCHANTID, trxStored.getMerchantId());
        Assertions.assertEquals("MERCHANTFISCALCODE0", trxStored.getMerchantFiscalCode());
        Assertions.assertEquals("VAT0", trxStored.getVat());
        Assertions.assertEquals(authPaymentDTO.getInitiativeId(), trxStored.getInitiativeId());
        Assertions.assertEquals("INITIATIVENAME", trxStored.getInitiativeName());
        Assertions.assertEquals("BUSINESSNAME", trxStored.getBusinessName());
        Assertions.assertEquals(USERID, trxStored.getUserId());
        Assertions.assertEquals(authPaymentDTO.getStatus(), trxStored.getStatus());
        Assertions.assertEquals(getChannel(), trxStored.getChannel());

        if (!expectedRewarded) {
            Assertions.assertEquals(SyncTrxStatus.REJECTED, trxStored.getStatus());
        }

        switch (trxStored.getStatus()) {
            case CREATED -> {
                Assertions.assertNull(trxStored.getTrxChargeDate());
                Assertions.assertNull(trxStored.getElaborationDateTime());
                Assertions.assertNull(trxStored.getReward());
                Assertions.assertEquals(Collections.emptyList(), trxStored.getRejectionReasons());
                Assertions.assertEquals(Collections.emptyMap(), trxStored.getRewards());
            }
            case REJECTED -> {
                Assertions.assertNull(trxStored.getElaborationDateTime());
                Assertions.assertEquals(0L, trxStored.getReward());
                Assertions.assertNotEquals(Collections.emptyList(), trxStored.getRejectionReasons());
                Assertions.assertEquals(Collections.emptyMap(), trxStored.getRewards());
            }
            case IDENTIFIED, AUTHORIZED -> {
                Assertions.assertNotNull(trxStored.getReward());
                Assertions.assertNotEquals(0L, trxStored.getReward());
                Assertions.assertEquals(Collections.emptyList(), trxStored.getRejectionReasons());
                Assertions.assertNotNull(trxStored.getRewards());
                Assertions.assertFalse(trxStored.getRewards().isEmpty());

                if (trxStored.getStatus().equals(SyncTrxStatus.AUTHORIZED)) {
                    Assertions.assertNotNull(trxStored.getTrxChargeDate());
                } else {
                    Assertions.assertNull(trxStored.getTrxChargeDate());
                }
                Assertions.assertNull(trxStored.getElaborationDateTime());
            }
            default -> throw new IllegalStateException("Unexpected stored status:" + trxStored.getStatus());
        }
    }

}
