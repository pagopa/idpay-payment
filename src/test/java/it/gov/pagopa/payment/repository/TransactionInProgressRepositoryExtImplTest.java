package it.gov.pagopa.payment.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.payment.BaseIntegrationTest;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
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

@Slf4j
class TransactionInProgressRepositoryExtImplTest extends BaseIntegrationTest {

  @Autowired protected TransactionInProgressRepository transactionInProgressRepository;

  @Autowired protected MongoTemplate mongoTemplate;

  @AfterEach
  void clearTestData(){
    mongoTemplate.findAllAndRemove(
        new Query(Criteria.where(TransactionInProgress.Fields.id).regex("^MOCKEDTRANSACTION_qr-code_[0-9]+$")),
        TransactionInProgress.class);
  }

  @Test
  void createIfExists() {

    TransactionInProgress transactionInProgress = TransactionInProgressFaker.mockInstance(1);
    TransactionInProgress transactionInProgressSecond = TransactionInProgressFaker.mockInstance(2);
    transactionInProgressSecond.setTrxCode(transactionInProgress.getTrxCode());

    UpdateResult updatedFirst =
        transactionInProgressRepository.createIfExists(
            transactionInProgress, transactionInProgress.getTrxCode());
    Assertions.assertNotNull(updatedFirst.getUpsertedId());
    Assertions.assertEquals(0L, updatedFirst.getMatchedCount());

    UpdateResult updatedSecond =
        transactionInProgressRepository.createIfExists(
            transactionInProgressSecond, transactionInProgress.getTrxCode());
    Assertions.assertNull(updatedSecond.getUpsertedId());
    Assertions.assertEquals(1L, updatedSecond.getMatchedCount());

    TransactionInProgress result =
        mongoTemplate.findById(transactionInProgress.getId(), TransactionInProgress.class);
    Assertions.assertNotNull(result);
    TestUtils.checkNotNullFields(result, "hpan", "userId", "authDate", "elaborationDateTime", "reward", "rejectionReasons");
    Assertions.assertEquals(transactionInProgress, result);
  }

  @Test
  void findByTrxCode(){

    TransactionInProgress transactionInProgress = TransactionInProgressFaker.mockInstance(1);

    transactionInProgressRepository.save(transactionInProgress);

    TransactionInProgress resultFirstSave = transactionInProgressRepository.findByTrxCode("TRXCODE1");
    Assertions.assertNotNull(resultFirstSave);
    TestUtils.checkNotNullFields(resultFirstSave, "hpan", "userId", "authDate", "elaborationDateTime", "reward", "rejectionReasons");

    transactionInProgress.setTrxChargeDate(LocalDateTime.now().minusMinutes(30));
    transactionInProgressRepository.save(transactionInProgress);

    TransactionInProgress resultSecondSave = transactionInProgressRepository.findByTrxCode("TRXCODE1");
    Assertions.assertNull(resultSecondSave);

  }

  @Test
  void updateTrxIdentified(){

    TransactionInProgress transactionInProgress = TransactionInProgressFaker.mockInstance(1);
    transactionInProgressRepository.save(transactionInProgress);

    TransactionInProgress resultFirstSave = transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
    Assertions.assertNotNull(resultFirstSave);
    TestUtils.checkNotNullFields(resultFirstSave, "hpan", "userId", "authDate", "elaborationDateTime", "reward", "rejectionReasons");

    transactionInProgressRepository.updateTrxIdentified("MOCKEDTRANSACTION_qr-code_1", "USERID1");
    TransactionInProgress resultSecondSave = transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
    Assertions.assertNotNull(resultSecondSave);
    TestUtils.checkNotNullFields(resultSecondSave, "hpan", "authDate", "elaborationDateTime", "reward", "rejectionReasons");
    Assertions.assertEquals(SyncTrxStatus.IDENTIFIED, resultSecondSave.getStatus());
    Assertions.assertEquals("USERID1", resultSecondSave.getUserId());

  }

  @Test
  void updateTrxRejected(){

    TransactionInProgress transactionInProgress = TransactionInProgressFaker.mockInstance(1);
    transactionInProgressRepository.save(transactionInProgress);

    TransactionInProgress resultFirstSave = transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
    Assertions.assertNotNull(resultFirstSave);
    TestUtils.checkNotNullFields(resultFirstSave, "hpan", "userId", "authDate", "elaborationDateTime", "reward", "rejectionReasons");

    transactionInProgressRepository.updateTrxRejected(
        "MOCKEDTRANSACTION_qr-code_1", "USERID1", List.of("REJECTIONREASON1"));
    TransactionInProgress resultSecondSave = transactionInProgressRepository.findById("MOCKEDTRANSACTION_qr-code_1").orElse(null);
    Assertions.assertNotNull(resultSecondSave);
    TestUtils.checkNotNullFields(resultSecondSave, "hpan", "authDate", "elaborationDateTime", "reward");
    Assertions.assertEquals(SyncTrxStatus.REJECTED, resultSecondSave.getStatus());
    Assertions.assertEquals("USERID1", resultSecondSave.getUserId());

  }

//  @Test
//  void findAndModify(){
//    TransactionInProgress notFoundResult = transactionInProgressRepository.findAndModify("DUMMYID");
//    Assertions.assertNull(notFoundResult);
//
//    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1);
//    mongoTemplate.insert(transaction);
//
//    TransactionInProgress result = transactionInProgressRepository.findAndModify(transaction.getTrxCode());
//
//    Assertions.assertNotNull(result.getAuthDate());
//    Assertions.assertEquals(transaction.getTrxCode(), result.getTrxCode());
//    assertTrue(result.getTrxChargeDate().isAfter(LocalDateTime.now().minusMinutes(15)));
//  }
}
