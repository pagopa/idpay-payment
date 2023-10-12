package it.gov.pagopa.payment.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.common.mongo.MongoTestUtilitiesService;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.common.web.exception.ClientException;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.BaseIntegrationTest;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class TransactionInProgressRepositoryExtImplTest extends BaseIntegrationTest {

    private static final String INITIATIVE_ID = "INITIATIVEID1";
    private static final String MERCHANT_ID = "MERCHANTID1";
    private static final String USER_ID = "USERID1";
    public static final int EXPIRATION_MINUTES = 4350;
    private static final String TRX_ID = "TRX_ID";

    @Autowired
    protected TransactionInProgressRepository transactionInProgressRepository;
    @Autowired
    protected MongoTemplate mongoTemplate;

    @AfterEach
    void clearTestData() {
        mongoTemplate.findAllAndRemove(
                new Query(
                        Criteria.where(TransactionInProgress.Fields.id)
                                .regex("^MOCKEDTRANSACTION_qr-code_[0-9]+$")),
                TransactionInProgress.class);
    }

   @Test
    void createIfExists() {

        TransactionInProgress transactionInProgress =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        TransactionInProgress transactionInProgressSecond =
                TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.CREATED);
        transactionInProgressSecond.setTrxCode(transactionInProgress.getTrxCode());

        UpdateResult updatedFirst =
                transactionInProgressRepository.createIfExists(
                        transactionInProgress, transactionInProgress.getTrxCode());
        assertNotNull(updatedFirst.getUpsertedId());
        assertEquals(0L, updatedFirst.getMatchedCount());

        UpdateResult updatedSecond =
                transactionInProgressRepository.createIfExists(
                        transactionInProgressSecond, transactionInProgress.getTrxCode());
        assertNull(updatedSecond.getUpsertedId());
        assertEquals(1L, updatedSecond.getMatchedCount());

        TransactionInProgress result =
                mongoTemplate.findById(transactionInProgress.getId(), TransactionInProgress.class);
        assertNotNull(result);
        TestUtils.checkNotNullFields(
                result,
                "userId",
                "authDate",
                "elaborationDateTime",
                "reward",
                "rejectionReasons",
                "rewards",
                "trxChargeDate");
    }

    @Test
    void findByTrxCodeThrottled() {
        TransactionInProgress notFoundResult = transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpiredThrottled("DUMMYID", EXPIRATION_MINUTES);
        assertNull(notFoundResult);

        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        transactionInProgressRepository.save(transaction);

        TransactionInProgress result = transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpiredThrottled(transaction.getTrxCode(), EXPIRATION_MINUTES);

        assertNotNull(result);
        assertEquals(transaction.getTrxCode(), result.getTrxCode());
        assertTrue(result.getTrxChargeDate().isAfter(OffsetDateTime.now().minusMinutes(EXPIRATION_MINUTES)));
        TestUtils.checkNotNullFields(
                result, "userId", "elaborationDateTime", "reward", "rejectionReasons", "rewards", "authDate", "trxChargeDate");

        ClientException exception =
                assertThrows(
                        ClientException.class,
                        () -> transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpiredThrottled("trxcode1", EXPIRATION_MINUTES));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getHttpStatus());
    }


    @Test
    void findByTrxCodeThrottled_Concurrent() {
        int N = 10;
        TransactionInProgress stored =
                transactionInProgressRepository.save(
                        TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.IDENTIFIED));

        Map<String, List<Map.Entry<MongoTestUtilitiesService.MongoCommand, Long>>> mongoCommandsByType = executeConcurrentLocks(N,
                () -> {
                    try {
                        transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpiredThrottled(stored.getTrxCode(), EXPIRATION_MINUTES);
                        return true;
                    } catch (ClientExceptionNoBody e) {
                        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getHttpStatus());
                        return false;
                    }
                }
        );
        Assertions.assertEquals(N-1, mongoCommandsByType.get("aggregate").get(0).getValue());
    }

    @Test
    void updateTrxAuthorized() {
        Long reward = 200L;
        TransactionInProgress transaction =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        transaction.setUserId("USERID%d".formatted(1));
        transactionInProgressRepository.save(transaction);

        transactionInProgressRepository.updateTrxAuthorized(transaction, reward, List.of());
        TransactionInProgress result =
                transactionInProgressRepository.findById(transaction.getId()).orElse(null);

        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(
                result,
                "authDate",
                "elaborationDateTime",
                "reward",
                "rejectionReasons",
                "rewards",
                "trxChargeDate");
        Assertions.assertEquals(SyncTrxStatus.AUTHORIZED, result.getStatus());

        transactionInProgressRepository.updateTrxAuthorized(transaction, reward, List.of());
    }

    @Test
    void updateTrxAuthorized_barCode() {
        Long reward = 200L;
        TransactionInProgress transaction =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        transaction.setUserId("USERID%d".formatted(1));
        transaction.setChannel(RewardConstants.TRX_CHANNEL_BARCODE);
        transactionInProgressRepository.save(transaction);

        transactionInProgressRepository.updateTrxAuthorized(transaction, reward, List.of());
        TransactionInProgress result =
                transactionInProgressRepository.findById(transaction.getId()).orElse(null);

        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(
                result,
                "authDate",
                "elaborationDateTime",
                "reward",
                "rejectionReasons",
                "rewards",
                "trxChargeDate");
        Assertions.assertEquals(SyncTrxStatus.AUTHORIZED, result.getStatus());

        transactionInProgressRepository.updateTrxAuthorized(transaction, reward, List.of());
    }

    @Test
    void findByTrxCode() {

        TransactionInProgress transactionInProgress =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        transactionInProgressRepository.save(transactionInProgress);

        TransactionInProgress resultFirstSave = transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpired("trxcode1", EXPIRATION_MINUTES);
        Assertions.assertNotNull(resultFirstSave);
        TestUtils.checkNotNullFields(
                resultFirstSave,
                "userId",
                "authDate",
                "elaborationDateTime",
                "reward",
                "rejectionReasons",
                "rewards",
                "trxChargeDate");

        transactionInProgress.setTrxDate(OffsetDateTime.now().minusMinutes(EXPIRATION_MINUTES));
        transactionInProgressRepository.save(transactionInProgress);

        TransactionInProgress resultSecondSave =
                transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpired("trxcode1", EXPIRATION_MINUTES);
        Assertions.assertNull(resultSecondSave);
    }

    @Test
    void updateTrxRelateUserIdentified() {
        // Given
        TransactionInProgress trx =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        transactionInProgressRepository.save(trx);

        transactionInProgressRepository.updateTrxRelateUserIdentified(trx.getId(),
                USER_ID, "IDPAYCODE");

        // When
        TransactionInProgress resultUpdate =
                transactionInProgressRepository.findById(trx.getId()).orElse(null);

        // Then
        Assertions.assertNotNull(resultUpdate);
        TestUtils.checkNotNullFields(resultUpdate,
                "authDate",
                "elaborationDateTime",
                "reward",
                "rejectionReasons",
                "rewards",
                "trxChargeDate");
        Assertions.assertEquals(SyncTrxStatus.IDENTIFIED, resultUpdate.getStatus());
        Assertions.assertEquals(USER_ID, resultUpdate.getUserId());
    }

    @Test
    void updateTrxIdentified() {

        TransactionInProgress transactionInProgress =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        transactionInProgressRepository.save(transactionInProgress);

        TransactionInProgress resultFirstSave =
                transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
        Assertions.assertNotNull(resultFirstSave);
        TestUtils.checkNotNullFields(
                resultFirstSave,
                "userId",
                "authDate",
                "elaborationDateTime",
                "reward",
                "rejectionReasons",
                "rewards",
                "trxChargeDate");

        transactionInProgressRepository.updateTrxIdentified("MOCKEDTRANSACTION_qr-code_1", "USERID1", 500L, List.of("REASON"), Map.of("ID", new Reward()), "CHANNEL" );
        TransactionInProgress resultSecondSave =
                transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
        Assertions.assertNotNull(resultSecondSave);
        TestUtils.checkNotNullFields(
                resultSecondSave, "authDate", "elaborationDateTime", "trxChargeDate");
        Assertions.assertEquals(SyncTrxStatus.IDENTIFIED, resultSecondSave.getStatus());
        Assertions.assertEquals("USERID1", resultSecondSave.getUserId());
    }

    @Test
    void updateTrxRejected() {

        TransactionInProgress transactionInProgress =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        transactionInProgressRepository.save(transactionInProgress);

        TransactionInProgress resultFirstSave =
                transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
        Assertions.assertNotNull(resultFirstSave);
        TestUtils.checkNotNullFields(
                resultFirstSave,
                "userId",
                "authDate",
                "elaborationDateTime",
                "reward",
                "rejectionReasons",
                "rewards",
                "trxChargeDate");

        transactionInProgressRepository.updateTrxRejected(
                "MOCKEDTRANSACTION_qr-code_1", "USERID1", List.of("REJECTIONREASON1"), "CHANNEL");
        TransactionInProgress resultSecondSave =
                transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
        Assertions.assertNotNull(resultSecondSave);
        TestUtils.checkNotNullFields(resultSecondSave, "authDate", "elaborationDateTime", "reward", "rewards", "trxChargeDate");
        Assertions.assertEquals(SyncTrxStatus.REJECTED, resultSecondSave.getStatus());
        Assertions.assertEquals("USERID1", resultSecondSave.getUserId());
    }

    @Test
    void testFindByIdThrottled() {
        TransactionInProgress notFoundResult =
                transactionInProgressRepository.findByIdThrottled("DUMMYID");
        Assertions.assertNull(notFoundResult);

        TransactionInProgress stored =
                transactionInProgressRepository.save(
                        TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.CREATED));
        String trxId = stored.getId();

        TransactionInProgress result = transactionInProgressRepository.findByIdThrottled(trxId);

        Assertions.assertNotNull(result.getElaborationDateTime());
        result.setElaborationDateTime(null);

        stored.setUpdateDate(stored.getUpdateDate().truncatedTo(ChronoUnit.MINUTES));
        result.setUpdateDate(result.getUpdateDate().truncatedTo(ChronoUnit.MINUTES));
        Assertions.assertEquals(stored, result);

        try {
            transactionInProgressRepository.findByIdThrottled(trxId);
            Assertions.fail("Expected exception");
        } catch (ClientExceptionNoBody e) {
            Assertions.assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getHttpStatus());
        }
    }

    @Test
    void testFindByIdThrottled_Concurrent() {
        int N = 10;
        TransactionInProgress stored =
                transactionInProgressRepository.save(
                        TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.CREATED));

        Map<String, List<Map.Entry<MongoTestUtilitiesService.MongoCommand, Long>>> mongoCommandsByType = executeConcurrentLocks(N,
                () -> {
                    try {
                        transactionInProgressRepository.findByIdThrottled(stored.getId());
                        return true;
                    } catch (ClientExceptionNoBody e) {
                        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getHttpStatus());
                        return false;
                    }
                }
        );
        Assertions.assertEquals(N-1, mongoCommandsByType.get("aggregate").get(0).getValue());
    }

    @Test
    void findByFilter() {
        TransactionInProgress transactionInProgress =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        transactionInProgress.setUserId(USER_ID);
        transactionInProgressRepository.save(transactionInProgress);
        Criteria criteria = transactionInProgressRepository.getCriteria(MERCHANT_ID, INITIATIVE_ID, USER_ID, SyncTrxStatus.IDENTIFIED.toString());
        Pageable paging = PageRequest.of(0, 10);
        List<TransactionInProgress> transactionInProgressList = transactionInProgressRepository.findByFilter(criteria, paging);
        assertEquals(transactionInProgress, transactionInProgressList.get(0));
    }

    @Test
    void getCount() {
        TransactionInProgress transactionInProgress1 =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.REJECTED);
        TransactionInProgress transactionInProgress2 =
                TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.CREATED);
        transactionInProgress2.setInitiativeId(INITIATIVE_ID);
        transactionInProgress2.setMerchantId(MERCHANT_ID);
        TransactionInProgress transactionInProgress3 =
                TransactionInProgressFaker.mockInstance(3, SyncTrxStatus.AUTHORIZED);
        transactionInProgress3.setInitiativeId(INITIATIVE_ID);
        transactionInProgress3.setMerchantId(MERCHANT_ID);
        transactionInProgressRepository.save(transactionInProgress1);
        transactionInProgressRepository.save(transactionInProgress2);
        transactionInProgressRepository.save(transactionInProgress3);
        Criteria criteria = transactionInProgressRepository.getCriteria(MERCHANT_ID, INITIATIVE_ID, null, null);
        long count = transactionInProgressRepository.getCount(criteria);
        assertEquals(3, count);
    }

    @Test
    void getCriteria() {
        Criteria criteria = transactionInProgressRepository.getCriteria(MERCHANT_ID, INITIATIVE_ID, USER_ID, SyncTrxStatus.AUTHORIZED.toString());
        assertEquals(4, criteria.getCriteriaObject().size());
    }

    @Test
    void findAuthorizationExpiredTransaction() {
        LocalDateTime now = LocalDateTime.now();
        // Not expired transaction
        TransactionInProgress transaction =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        transactionInProgressRepository.save(transaction);

        TransactionInProgress notExpiredTrxResult = transactionInProgressRepository.findAuthorizationExpiredTransaction(null, EXPIRATION_MINUTES);
        Assertions.assertNull(notExpiredTrxResult);

        // expired transaction
        TransactionInProgress transactionExpired =
                TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.CREATED);
        transactionExpired.setTrxDate(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS).minusMinutes(EXPIRATION_MINUTES));
        transactionInProgressRepository.save(transactionExpired);

        TransactionInProgress expiredTrxResult = transactionInProgressRepository.findAuthorizationExpiredTransaction(null, EXPIRATION_MINUTES);
        Assertions.assertNotNull(expiredTrxResult);
        assertElaborationsDateTime(now, expiredTrxResult);
        Assertions.assertEquals(transactionExpired, expiredTrxResult);
        Assertions.assertNull(transactionInProgressRepository.findAuthorizationExpiredTransaction("DUMMYINITIATIVEID", EXPIRATION_MINUTES));

        // throttled test
        TransactionInProgress expiredTrxThrottledResult = transactionInProgressRepository.findAuthorizationExpiredTransaction(null, EXPIRATION_MINUTES);
        Assertions.assertNull(expiredTrxThrottledResult);

    }

    @Test
    void testFindAuthorizationExpiredTransaction_concurrent() {
        TransactionInProgress transactionExpired =
                TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.CREATED);
        transactionExpired.setTrxDate(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS).minusMinutes(EXPIRATION_MINUTES));
        transactionInProgressRepository.save(transactionExpired);

        executeConcurrentLocks(10, () -> transactionInProgressRepository.findAuthorizationExpiredTransaction(null, EXPIRATION_MINUTES) != null);
    }

    @Test
    void findCancelExpiredTransaction() {
        LocalDateTime now = LocalDateTime.now();
        // Not expired transaction
        TransactionInProgress transaction =
                TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        transactionInProgressRepository.save(transaction);

        TransactionInProgress notExpiredTrxResult = transactionInProgressRepository.findCancelExpiredTransaction(null, EXPIRATION_MINUTES);
        Assertions.assertNull(notExpiredTrxResult);

        // expired transaction
        TransactionInProgress transactionExpired =
                TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.AUTHORIZED);
        transactionExpired.setTrxDate(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS).minusMinutes(EXPIRATION_MINUTES));
        transactionInProgressRepository.save(transactionExpired);

        TransactionInProgress expiredTrxResult = transactionInProgressRepository.findCancelExpiredTransaction(null, EXPIRATION_MINUTES);
        Assertions.assertNotNull(expiredTrxResult);
        assertElaborationsDateTime(now, expiredTrxResult);
        Assertions.assertEquals(transactionExpired, expiredTrxResult);
        Assertions.assertNull(transactionInProgressRepository.findCancelExpiredTransaction("DUMMYINITIATIVEID", EXPIRATION_MINUTES));

        // throttled test
        TransactionInProgress expiredTrxThrottledResult = transactionInProgressRepository.findCancelExpiredTransaction(null, EXPIRATION_MINUTES);
        Assertions.assertNull(expiredTrxThrottledResult);

    }

    @Test
    void testFindCancelExpiredTransaction_concurrent() {
        TransactionInProgress transactionExpired =
                TransactionInProgressFaker.mockInstance(2, SyncTrxStatus.AUTHORIZED);
        transactionExpired.setTrxDate(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS).minusMinutes(EXPIRATION_MINUTES));
        transactionInProgressRepository.save(transactionExpired);

        executeConcurrentLocks(10, () -> transactionInProgressRepository.findCancelExpiredTransaction(null, EXPIRATION_MINUTES) != null);
    }

    @Test
    void deletePaged (){
        // Given
        int pageSize = 100;
        TransactionInProgress transactionInProgress = TransactionInProgress.builder()
                .id(TRX_ID)
                .initiativeId(INITIATIVE_ID)
                .build();
        mongoTemplate.save(transactionInProgress);

        // When
        List<TransactionInProgress> result = transactionInProgressRepository.deletePaged(INITIATIVE_ID, pageSize);

        // Then
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(transactionInProgress.getId(), result.get(0).getId());
        Assertions.assertEquals(transactionInProgress.getInitiativeId(), result.get(0).getInitiativeId());
    }

    private void assertElaborationsDateTime(LocalDateTime now, TransactionInProgress trx) {
        long minutes = Duration.between(now, trx.getElaborationDateTime()).toMinutes();
        assertTrue(minutes <= 1);
        trx.setElaborationDateTime(null);
    }

    private Map<String, List<Map.Entry<MongoTestUtilitiesService.MongoCommand, Long>>> executeConcurrentLocks(int attempts, Supplier<Boolean> lockAcquirer){
        AtomicInteger dropped = new AtomicInteger(0);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            MongoTestUtilitiesService.startMongoCommandListener("lockingQuery_Concurrent");
            List<Future<Integer>> tasks =
                    IntStream.range(0, attempts)
                            .mapToObj(i -> executorService.submit(() -> {
                                if (lockAcquirer.get()) {
                                    return 1;
                                } else {
                                    dropped.incrementAndGet();
                                    return 0;
                                }
                            }))
                            .toList();
            int successfulLocks = tasks.stream().mapToInt(f -> {
                try {
                    return f.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new IllegalStateException(e);
                }
            }).sum();
            List<Map.Entry<MongoTestUtilitiesService.MongoCommand, Long>> commands = MongoTestUtilitiesService.stopAndGetMongoCommands();
            MongoTestUtilitiesService.printMongoCommands(commands);

            Assertions.assertEquals(1, successfulLocks);
            Assertions.assertEquals(attempts - 1, dropped.get());

            Map<String, List<Map.Entry<MongoTestUtilitiesService.MongoCommand, Long>>> groupByCommand = commands.stream().collect(Collectors.groupingBy(c -> c.getKey().getType()));
            Assertions.assertEquals(attempts, groupByCommand.get("findAndModify").get(0).getValue());

            return groupByCommand;
        } finally {
            executorService.shutdown();
        }
    }


}
