package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.RewardFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.utils.TestUtils;
import it.gov.pagopa.payment.utils.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthPaymentMapperTest {

 private AuthPaymentMapper mapper;

 @BeforeEach
 void setUp() {
   mapper = new AuthPaymentMapper();
 }

 @Test
 void transactionMapperTest(){
   TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1,
       SyncTrxStatus.AUTHORIZED);
   Reward reward = RewardFaker.mockInstance(1);
   transaction.setReward(Utils.euroToCents(reward.getAccruedReward()));
   AuthPaymentDTO result = mapper.transactionMapper(transaction);
   assertAll(() -> {
     assertNotNull(result);
     assertEquals(transaction.getId(), result.getId());
     assertEquals(transaction.getReward(), result.getReward());
     assertEquals(transaction.getInitiativeId(), result.getInitiativeId());
     assertEquals(transaction.getRejectionReasons(), result.getRejectionReasons());
     assertEquals(transaction.getStatus(), result.getStatus());
     assertEquals(transaction.getTrxCode(), result.getTrxCode());
     assertEquals(transaction.getAmountCents(), result.getAmountCents());
     TestUtils.checkNotNullFields(result);
   });

 }
}
