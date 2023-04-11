package it.gov.pagopa.payment.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.payment.BaseIntegrationTest;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

@Slf4j
class TransactionInProgressRepositoryImplTest extends BaseIntegrationTest {

  @Autowired protected TransactionInProgressRepositoryImpl transactionInProgressRepository;

  @Autowired protected MongoTemplate mongoTemplate;

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
}
