package it.gov.pagopa.payment.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.payment.BaseIntegrationTest;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.common.web.exception.ClientException;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.common.utils.TestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

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

@Slf4j
class TransactionInProgressRepositoryExtImplTest extends BaseIntegrationTest {

    private static final String INITIATIVE_ID = "INITIATIVEID1";
    private static final String MERCHANT_ID = "MERCHANTID1";
    private static final String USER_ID = "USERID1";
    public static final int EXPIRATION_MINUTES = 4350;

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
                "rewards");
    }

    @Test
    void findByTrxCodeThrottled() {
        TransactionInProgress notFoundResult = transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpiredThrottled("DUMMYID", EXPIRATION_MINUTES);
        assertNull(notFoundResult);

        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        transaction.setTrxChargeDate(null);
        transactionInProgressRepository.save(transaction);

        TransactionInProgress result = transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpiredThrottled(transaction.getTrxCode(), EXPIRATION_MINUTES);

        assertNotNull(result);
        assertEquals(transaction.getTrxCode(), result.getTrxCode());
        assertTrue(result.getTrxChargeDate().isAfter(OffsetDateTime.now().minusMinutes(EXPIRATION_MINUTES)));
        TestUtils.checkNotNullFields(
                result, "userId", "elaborationDateTime", "reward", "rejectionReasons", "rewards");

        ClientException exception =
                assertThrows(
                        ClientException.class,
                        () -> transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpiredThrottled("trxcode1", EXPIRATION_MINUTES));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getHttpStatus());
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
                "rewards");
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
                "rewards");

        transactionInProgress.setTrxDate(OffsetDateTime.now().minusMinutes(EXPIRATION_MINUTES));
        transactionInProgressRepository.save(transactionInProgress);

        TransactionInProgress resultSecondSave =
                transactionInProgressRepository.findByTrxCodeAndAuthorizationNotExpired("trxcode1", EXPIRATION_MINUTES);
        Assertions.assertNull(resultSecondSave);
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
                "rewards");

        transactionInProgressRepository.updateTrxIdentified("MOCKEDTRANSACTION_qr-code_1", "USERID1", 500L, List.of("REASON"), Map.of("ID", new Reward()));
        TransactionInProgress resultSecondSave =
                transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
        Assertions.assertNotNull(resultSecondSave);
        TestUtils.checkNotNullFields(
                resultSecondSave, "authDate", "elaborationDateTime");
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
                "rewards");

        transactionInProgressRepository.updateTrxRejected(
                "MOCKEDTRANSACTION_qr-code_1", "USERID1", List.of("REJECTIONREASON1"));
        TransactionInProgress resultSecondSave =
                transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
        Assertions.assertNotNull(resultSecondSave);
        TestUtils.checkNotNullFields(resultSecondSave, "authDate", "elaborationDateTime", "reward", "rewards");
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

    private void assertElaborationsDateTime(LocalDateTime now, TransactionInProgress trx) {
        long minutes = Duration.between(now, trx.getElaborationDateTime()).toMinutes();
        Assertions.assertTrue(minutes <= 1);
        trx.setElaborationDateTime(null);
    }

}
