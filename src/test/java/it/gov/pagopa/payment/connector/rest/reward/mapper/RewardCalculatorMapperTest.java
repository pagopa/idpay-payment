package it.gov.pagopa.payment.connector.rest.reward.mapper;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.PaymentRequestDTO;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.AuthPaymentResponseDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

 class RewardCalculatorMapperTest {

  private RewardCalculatorMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new RewardCalculatorMapper();
  }

  @Test
  void preAuthRquestMap() {

    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1,
        SyncTrxStatus.IDENTIFIED);
    transaction.setUserId("USERID%d".formatted(1));
    PaymentRequestDTO result = mapper.preAuthRequestMap(transaction);

      commonAssertField(transaction, result);
  }
     @Test
     void authRquestMap() {

         TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1,
                 SyncTrxStatus.IDENTIFIED);
         transaction.setUserId("USERID%d".formatted(1));
         transaction.setCounterVersion(100L);
         AuthPaymentRequestDTO result = mapper.authRequestMap(transaction);

         commonAssertField(transaction,result);
         assertEquals(transaction.getCounterVersion(),result.getRewardCents());
     }

  @Test
  void rewardResponseMap() {
    AuthPaymentResponseDTO responseDTO = AuthPaymentResponseDTOFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    transaction.setRejectionReasons(List.of());
    transaction.setRewardCents(0L);

    AuthPaymentDTO result = mapper.rewardResponseMap(responseDTO, transaction);
    assertAll(() -> {
      assertNotNull(result);
      assertEquals(responseDTO.getTransactionId(), result.getId());
      assertEquals(responseDTO.getReward().getAccruedRewardCents(), result.getRewardCents());
      assertEquals(responseDTO.getInitiativeId(), result.getInitiativeId());
      assertEquals(responseDTO.getRejectionReasons(), result.getRejectionReasons());
      assertEquals(responseDTO.getStatus(), result.getStatus());
      assertEquals(transaction.getTrxCode(), result.getTrxCode());
      assertEquals(Map.of(responseDTO.getInitiativeId(), responseDTO.getReward()), result.getRewards());
      assertEquals(responseDTO.getReward().getCounters(), result.getCounters());
      assertEquals(responseDTO.getCounterVersion(),result.getCounterVersion());
        TestUtils.checkNotNullFields(result,"residualBudgetCents", "secondFactor","splitPayment",
                "residualAmountCents");
    });
  }

   @Test
   void rewardResponseMapNullReward() {
     AuthPaymentResponseDTO responseDTO = AuthPaymentResponseDTOFaker.mockInstance(1,
         SyncTrxStatus.REJECTED);
     responseDTO.setReward(null);
     TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1,
         SyncTrxStatus.REJECTED);
     transaction.setRejectionReasons(List.of());

     AuthPaymentDTO result = mapper.rewardResponseMap(responseDTO, transaction);
     assertAll(() -> {
       assertNotNull(result);
       assertEquals(responseDTO.getTransactionId(), result.getId());
       assertEquals(0L, result.getRewardCents());
       assertEquals(responseDTO.getInitiativeId(), result.getInitiativeId());
       assertEquals(responseDTO.getRejectionReasons(), result.getRejectionReasons());
       assertEquals(responseDTO.getStatus(), result.getStatus());
       assertEquals(transaction.getTrxCode(), result.getTrxCode());
       assertNull(result.getCounters());
       assertEquals(Collections.emptyMap(), result.getRewards());
       TestUtils.checkNotNullFields(result, "counters","residualBudgetCents", "secondFactor","splitPayment",
               "residualAmountCents");
     });
   }

     @Test
     void rewardResponseMapNullRejectionReasons() {
         AuthPaymentResponseDTO responseDTO = AuthPaymentResponseDTOFaker.mockInstance(1,
                 SyncTrxStatus.REJECTED);
         TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1,
                 SyncTrxStatus.REJECTED);
         transaction.setRejectionReasons(null);

         AuthPaymentDTO result = mapper.rewardResponseMap(responseDTO, transaction);
         assertAll(() -> {
             assertNotNull(result);
             assertEquals(responseDTO.getTransactionId(), result.getId());
             assertEquals(responseDTO.getReward().getAccruedRewardCents(), result.getRewardCents());
             assertEquals(responseDTO.getInitiativeId(), result.getInitiativeId());
             assertEquals(Collections.emptyList(), result.getRejectionReasons());
             assertEquals(responseDTO.getStatus(), result.getStatus());
             assertEquals(transaction.getTrxCode(), result.getTrxCode());
             assertEquals(Map.of(responseDTO.getInitiativeId(), responseDTO.getReward()), result.getRewards());
             assertEquals(responseDTO.getReward().getCounters(), result.getCounters());
             TestUtils.checkNotNullFields(result, "residualBudgetCents", "secondFactor","splitPayment",
                     "residualAmountCents");
         });
     }

     private static void commonAssertField(TransactionInProgress transaction, PaymentRequestDTO result) {
         assertAll(() -> {
             assertNotNull(result);
             assertEquals(transaction.getId(), result.getTransactionId());
             assertEquals(transaction.getUserId(), result.getUserId());
             assertEquals(transaction.getMerchantId(), result.getMerchantId());
             assertEquals(transaction.getMerchantFiscalCode(), result.getMerchantFiscalCode());
             assertEquals(transaction.getVat(), result.getVat());
             assertEquals(transaction.getIdTrxIssuer(), result.getIdTrxIssuer());
             assertEquals(transaction.getTrxDate(), result.getTrxDate());
             assertEquals(transaction.getAmountCents(), result.getAmountCents());
             assertEquals(transaction.getAmountCurrency(), result.getAmountCurrency());
             assertEquals(transaction.getMcc(), result.getMcc());
             assertEquals(transaction.getAcquirerId(), result.getAcquirerId());
             assertEquals(transaction.getIdTrxAcquirer(), result.getIdTrxAcquirer());
             assertEquals(transaction.getTrxChargeDate(), result.getTrxChargeDate());
             assertEquals(transaction.getCorrelationId(), result.getTransactionId());
             TestUtils.checkNotNullFields(result, "trxChargeDate");
         });
     }

}
