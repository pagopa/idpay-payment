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
import it.gov.pagopa.payment.exception.ClientException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.RewardFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.utils.TestUtils;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;

@Slf4j
class TransactionInProgressRepositoryExtImplTest extends BaseIntegrationTest {

  @Autowired
  protected TransactionInProgressRepository transactionInProgressRepository;

  @Autowired
  protected MongoTemplate mongoTemplate;

  @AfterEach
  void clearTestData(){
    mongoTemplate.findAllAndRemove(
        new Query(Criteria.where(TransactionInProgress.Fields.id).regex("^MOCKEDTRANSACTION_qr-code_[0-9]+$")),
        TransactionInProgress.class);
  }


  @Test
  void createIfExists() {

    TransactionInProgress transactionInProgress = TransactionInProgressFaker.mockInstance(1,
        SyncTrxStatus.CREATED);
    TransactionInProgress transactionInProgressSecond = TransactionInProgressFaker.mockInstance(2,
        SyncTrxStatus.CREATED);
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
    TestUtils.checkNotNullFields(result, "userId", "authDate", "elaborationDateTime", "reward", "rejectionReasons");
  }

  @Test
  void findByTrxCodeThrottled() {
    TransactionInProgress notFoundResult = transactionInProgressRepository.findByTrxCodeThrottled(
        "DUMMYID");
    assertNull(notFoundResult);

    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1,
        SyncTrxStatus.IDENTIFIED);
    transactionInProgressRepository.save(transaction);

    TransactionInProgress result = transactionInProgressRepository.findByTrxCodeThrottled(
        transaction.getTrxCode());

    assertNotNull(result);
    assertEquals(transaction.getTrxCode(), result.getTrxCode());
    assertTrue(result.getTrxChargeDate().isAfter(LocalDateTime.now().minusMinutes(15)));
    TestUtils.checkNotNullFields(result, "userId", "elaborationDateTime", "reward",
        "rejectionReasons");

    ClientException exception =
        assertThrows(ClientException.class,
            () -> transactionInProgressRepository.findByTrxCodeThrottled("TRXCODE1"));

      assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getHttpStatus());

  }

  @Test
  void updateTrxAuthorized() {
    Reward reward = RewardFaker.mockInstance(1);
    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1,
        SyncTrxStatus.IDENTIFIED);
    transaction.setUserId("USERID%d".formatted(1));
    transactionInProgressRepository.save(transaction);

    TransactionInProgress notFoundResult = transactionInProgressRepository.findById(
        "MOCKEDTRANSACTION_qr-code_1").orElse(null);
    Assertions.assertNull(notFoundResult);

    transactionInProgressRepository.updateTrxAuthorized(transaction.getId(), reward, List.of());
    TransactionInProgress result = transactionInProgressRepository.findById(
        transaction.getId()).orElse(null);

    Assertions.assertNotNull(result);
    TestUtils.checkNotNullFields(result, "hpan", "authDate", "elaborationDateTime",
        "reward", "rejectionReasons");
    Assertions.assertEquals(SyncTrxStatus.AUTHORIZED, result.getStatus());

    transactionInProgressRepository.updateTrxAuthorized(transaction.getId(), reward, List.of());

  }

  @Test
  void findByTrxCode(){

    TransactionInProgress transactionInProgress = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

    transactionInProgressRepository.save(transactionInProgress);

    TransactionInProgress resultFirstSave = transactionInProgressRepository.findByTrxCode("TRXCODE1");
    Assertions.assertNotNull(resultFirstSave);
    TestUtils.checkNotNullFields(resultFirstSave, "userId", "authDate", "elaborationDateTime", "reward", "rejectionReasons");

    transactionInProgress.setTrxChargeDate(LocalDateTime.now().minusMinutes(30));
    transactionInProgressRepository.save(transactionInProgress);

    TransactionInProgress resultSecondSave = transactionInProgressRepository.findByTrxCode("TRXCODE1");
    Assertions.assertNull(resultSecondSave);

  }

  @Test
  void updateTrxIdentified(){

    TransactionInProgress transactionInProgress = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    transactionInProgressRepository.save(transactionInProgress);

    TransactionInProgress resultFirstSave = transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
    Assertions.assertNotNull(resultFirstSave);
    TestUtils.checkNotNullFields(resultFirstSave, "userId", "authDate", "elaborationDateTime", "reward", "rejectionReasons");

    transactionInProgressRepository.updateTrxIdentified("MOCKEDTRANSACTION_qr-code_1", "USERID1");
    TransactionInProgress resultSecondSave = transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
    Assertions.assertNotNull(resultSecondSave);
    TestUtils.checkNotNullFields(resultSecondSave, "authDate", "elaborationDateTime", "reward", "rejectionReasons");
    Assertions.assertEquals(SyncTrxStatus.IDENTIFIED, resultSecondSave.getStatus());
    Assertions.assertEquals("USERID1", resultSecondSave.getUserId());

  }

  @Test
  void updateTrxRejected(){

    TransactionInProgress transactionInProgress = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
    transactionInProgressRepository.save(transactionInProgress);

    TransactionInProgress resultFirstSave = transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
    Assertions.assertNotNull(resultFirstSave);
    TestUtils.checkNotNullFields(resultFirstSave, "userId", "authDate", "elaborationDateTime", "reward", "rejectionReasons");

    transactionInProgressRepository.updateTrxRejected(
        "MOCKEDTRANSACTION_qr-code_1", "USERID1", List.of("REJECTIONREASON1"));
    TransactionInProgress resultSecondSave = transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
    Assertions.assertNotNull(resultSecondSave);
    TestUtils.checkNotNullFields(resultSecondSave, "authDate", "elaborationDateTime", "reward");
    Assertions.assertEquals(SyncTrxStatus.REJECTED, resultSecondSave.getStatus());
    Assertions.assertEquals("USERID1", resultSecondSave.getUserId());

  }
}
