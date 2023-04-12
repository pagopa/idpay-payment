package it.gov.pagopa.payment.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.payment.BaseIntegrationTest;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.RewardRule;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.TransactionCreationRequestFaker;
import org.apache.commons.lang3.function.FailableConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

abstract class BasePaymentControllerIntegrationTest extends BaseIntegrationTest {

    public static final String INITIATIVEID = "INITIATIVEID";
    public static final String USERID = "USERID";
    public static final String MERCHANTID = "MERCHANTID";

    private static final int parallelism = 8;
    private static final ExecutorService executor = Executors.newFixedThreadPool(parallelism);

    private static final int N = 100;

    private final List<FailableConsumer<Integer, Exception>> useCases = new ArrayList<>();

    @Autowired
    private RewardRuleRepository rewardRuleRepository;
    @Autowired
    private TransactionInProgressRepository transactionInProgressRepository;

    @Autowired
    private TransactionInProgress2TransactionResponseMapper transactionResponseMapper;

    @Value("${app.qrCode.throttlingSeconds}")
    private int throttlingSeconds;

    @Test
    void test() {
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
                Assertions.fail("UseCase %d (bias %d) failed: ".formatted(i % useCases.size(), i));
            }
        }
    }

    /**
     * Invoke create transaction API acting as <i>merchantId</i>
     */
    protected abstract MvcResult createTrx(TransactionCreationRequest trxRequest, String merchantId) throws Exception;

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
    protected abstract MvcResult confirmPayment(TransactionResponse trx, String merchantId) throws Exception;

    /**
     * Override in order to add specific use cases
     */
    protected List<FailableConsumer<Integer, Exception>> getExtraUseCases() {
        return Collections.emptyList();
    }

    private TransactionResponse createTrxSuccess(TransactionCreationRequest trxRequest) throws Exception {
        TransactionResponse trxCreated = extractResponse(createTrx(trxRequest, MERCHANTID), 204, TransactionResponse.class);
        Assertions.assertEquals(SyncTrxStatus.CREATED, trxCreated.getStatus());
        checkTransactionStored(trxCreated);
        return trxCreated;
    }

    private void checkTransactionStored(TransactionResponse trxCreated) {
        TransactionInProgress stored = transactionInProgressRepository.findById(trxCreated.getId()).orElse(null);
        Assertions.assertNotNull(stored);
        Assertions.assertEquals(trxCreated, transactionResponseMapper.apply(stored));
    }

    {
        // useCase 0: initiative not existent
        useCases.add(i -> {
            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId("DUMMYINITIATIVEID");

            extractResponse(createTrx(trxRequest, MERCHANTID), 404, null);

            // Other APIs cannot be invoked because we have not a valid trxId
            TransactionResponse dummyTrx = TransactionResponse.builder().id("DUMMYTRXID").trxCode("DUMMYTRXCODE").trxDate(LocalDateTime.now()).build();
            extractResponse(preAuthTrx(dummyTrx, USERID, MERCHANTID), 400, null);
            extractResponse(authTrx(dummyTrx, USERID, MERCHANTID), 400, null);
            extractResponse(confirmPayment(dummyTrx, MERCHANTID), 400, null);
        });

        // useCase 1: userId not onboarded
        useCases.add(i -> {
            String userIdNotOnboarded = "DUMMYUSERID";

            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId(INITIATIVEID);

            // Creating transaction
            TransactionResponse trxCreated = createTrxSuccess(trxRequest);

            // Cannot relate user because not onboarded
            TransactionResponse failedPreview = extractResponse(preAuthTrx(trxCreated, userIdNotOnboarded, MERCHANTID), 403, TransactionResponse.class);
            Assertions.assertEquals(SyncTrxStatus.REWARDED, failedPreview.getStatus()); // TODO fix expected status
//            Assertions.assertEquals(List.of("NO_ACTIVE..."), failedPreview.getRejectionReasons()); TODO assert rejection reason
            TransactionResponse failedPreviewResubmit = extractResponse(preAuthTrx(trxCreated, userIdNotOnboarded, MERCHANTID), 403, TransactionResponse.class);
            Assertions.assertEquals(failedPreview, failedPreviewResubmit);

            // Other APIs will fail because status not expected
            extractResponse(authTrx(trxCreated, userIdNotOnboarded, MERCHANTID), 400, null);
            extractResponse(confirmPayment(trxCreated, MERCHANTID), 400, null);

            checkTransactionStored(trxCreated);
        });

        // useCase 2: complete successful flow
        useCases.add(i -> {
            TransactionCreationRequest trxRequest = TransactionCreationRequestFaker.mockInstance(i);
            trxRequest.setInitiativeId(INITIATIVEID);

            // Creating transaction
            TransactionResponse trxCreated = createTrxSuccess(trxRequest);

            // Cannot invoke other APIs if not relating first
            extractResponse(authTrx(trxCreated, USERID, MERCHANTID), 400, null);
            extractResponse(confirmPayment(trxCreated, MERCHANTID), 400, null);

            // Relating to user
            TransactionResponse preAuthResult = extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), 200, TransactionResponse.class); // TODO fix return type
            Assertions.assertEquals(SyncTrxStatus.AUTHORIZED, preAuthResult.getStatus()); // TODO fix expected status
            // Relating to user resubmission
            TransactionResponse preAuthResultResubmitted = extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), 200, TransactionResponse.class); // TODO fix return type
            Assertions.assertEquals(preAuthResult, preAuthResultResubmitted);
            checkTransactionStored(preAuthResult);
            // Only the right userId could resubmit preview
            extractResponse(preAuthTrx(trxCreated, "DUMMYUSERID", MERCHANTID), 403, null);


            // Cannot invoke other APIs if not authorizing first
            extractResponse(confirmPayment(trxCreated, MERCHANTID), 400, null);

            // Only the right userId could authorize its transaction
            extractResponse(authTrx(trxCreated, "DUMMYUSERID", MERCHANTID), 403, null);

            // Authorizing transaction
            TransactionResponse authResult = extractResponse(authTrx(trxCreated, USERID, MERCHANTID), 200, TransactionResponse.class); // TODO fix return type
            // TooManyRequest behavior
            extractResponse(authTrx(trxCreated, USERID, MERCHANTID), 429, null);
            Assertions.assertEquals(SyncTrxStatus.AUTHORIZED, authResult.getStatus());
            // Cannot invoke preAuth after authorization
            extractResponse(preAuthTrx(trxCreated, USERID, MERCHANTID), 403, null);
            // Authorizing transaction resubmission after throttling time
            wait(throttlingSeconds, TimeUnit.SECONDS);
            TransactionResponse authResultResubmitted = extractResponse(authTrx(trxCreated, USERID, MERCHANTID), 409, TransactionResponse.class); // TODO fix return type
            Assertions.assertEquals(authResult, authResultResubmitted);
            checkTransactionStored(authResult);

            // Unexpected merchant trying to confirm
            extractResponse(confirmPayment(trxCreated, "DUMMYMERCHANTID"), 403, null);

            // Confirming payment
            TransactionResponse confirmResult = extractResponse(confirmPayment(trxCreated, MERCHANTID), 200, TransactionResponse.class);
            Assertions.assertEquals(SyncTrxStatus.REWARDED, confirmResult.getStatus());
            // Confirming payment resubmission
            extractResponse(confirmPayment(trxCreated, MERCHANTID), 404, null);

            Assertions.assertFalse(transactionInProgressRepository.existsById(trxCreated.getId()));
        });

        useCases.addAll(getExtraUseCases());
    }

    protected <T> T extractResponse(MvcResult response, int expectedHttpStatusCode, Class<T> expectedBodyClass) {
        Assertions.assertEquals(expectedHttpStatusCode, response.getResponse().getStatus());
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
