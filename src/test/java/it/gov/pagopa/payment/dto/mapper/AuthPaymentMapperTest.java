package it.gov.pagopa.payment.dto.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.RewardFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthPaymentMapperTest {

 private AuthPaymentMapper mapper;

 @BeforeEach
 void setUp() {
   mapper = new AuthPaymentMapper();
 }

 @Test
 void transactionMapperTest(){
   TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
   Reward reward = RewardFaker.mockInstance(1);
   transaction.setRewardCents(reward.getAccruedRewardCents());

   AuthPaymentDTO result = mapper.transactionMapper(transaction);

   assertAll(() -> {
     assertNotNull(result);
     assertEquals(transaction.getId(), result.getId());
     assertEquals(transaction.getRewardCents(), result.getRewardCents());
     assertEquals(transaction.getInitiativeId(), result.getInitiativeId());
     assertEquals(transaction.getRejectionReasons(), result.getRejectionReasons());
     assertEquals(transaction.getStatus(), result.getStatus());
     assertEquals(transaction.getTrxCode(), result.getTrxCode());
     assertEquals(transaction.getAmountCents(), result.getAmountCents());
       TestUtils.checkNotNullFields(result, "residualBudget", "secondFactor","splitPayment",
               "residualAmountCents");
   });

 }
}
