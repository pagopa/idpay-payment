package it.gov.pagopa.payment.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.payment.BaseIntegrationTest;
import it.gov.pagopa.payment.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;

import java.util.List;

@Slf4j
class TransactionInProgressRepositoryExtImplTest extends BaseIntegrationTest {

  @Autowired protected TransactionInProgressRepository transactionInProgressRepository;

  @Autowired protected MongoTemplate mongoTemplate;

  @AfterEach
  void clearTestData(){
    transactionInProgressRepository.deleteAllById(List.of(
            "MOCKEDTRANSACTION_qr-code_0",
            "MOCKEDTRANSACTION_qr-code_1",
            "MOCKEDTRANSACTION_qr-code_2"
            ));
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
    TestUtils.checkNotNullFields(result, "hpan", "merchantId", "userId", "authDate", "elaborationDateTime");
  }

  @Test
  void testFindByIdThrottled(){
    TransactionInProgress notFoundResult = transactionInProgressRepository.findByIdThrottled("DUMMYID");
    Assertions.assertNull(notFoundResult);

    TransactionInProgress stored = transactionInProgressRepository.save(TransactionInProgressFaker.mockInstance(0));
    String trxId = stored.getId();

    TransactionInProgress result = transactionInProgressRepository.findByIdThrottled(trxId);

    Assertions.assertEquals(stored, result);

    try{
      transactionInProgressRepository.findByIdThrottled(trxId);
      Assertions.fail("Expected exception");
    } catch (ClientExceptionNoBody e){
      Assertions.assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getHttpStatus());
    }
  }
}
