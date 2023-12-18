package it.gov.pagopa.payment.connector.rest.reward.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.AuthPaymentResponseDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

 class RewardCalculatorMapperTest {

  private RewardCalculatorMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new RewardCalculatorMapper();
  }

  @Test
  void rewardMap() {

    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1,
        SyncTrxStatus.IDENTIFIED);
    transaction.setUserId("USERID%d".formatted(1));
    AuthPaymentRequestDTO result = mapper.rewardMap(transaction);

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

  @Test
  void rewardResponseMap() {
    AuthPaymentResponseDTO responseDTO = AuthPaymentResponseDTOFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
    transaction.setRejectionReasons(List.of());
    transaction.setReward(0L);

    AuthPaymentDTO result = mapper.rewardResponseMap(responseDTO, transaction);
    assertAll(() -> {
      assertNotNull(result);
      assertEquals(responseDTO.getTransactionId(), result.getId());
      assertEquals(CommonUtilities.euroToCents(responseDTO.getReward().getAccruedReward()), result.getReward());
      assertEquals(responseDTO.getInitiativeId(), result.getInitiativeId());
      assertEquals(responseDTO.getRejectionReasons(), result.getRejectionReasons());
      assertEquals(responseDTO.getStatus(), result.getStatus());
      assertEquals(transaction.getTrxCode(), result.getTrxCode());
      assertEquals(Map.of(responseDTO.getInitiativeId(), responseDTO.getReward()), result.getRewards());
      assertEquals(responseDTO.getReward().getCounters(), result.getCounters());
        TestUtils.checkNotNullFields(result,"residualBudget", "secondFactor","splitPayment",
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
       assertEquals(0L, result.getReward());
       assertEquals(responseDTO.getInitiativeId(), result.getInitiativeId());
       assertEquals(responseDTO.getRejectionReasons(), result.getRejectionReasons());
       assertEquals(responseDTO.getStatus(), result.getStatus());
       assertEquals(transaction.getTrxCode(), result.getTrxCode());
       assertNull(result.getCounters());
       assertEquals(Collections.emptyMap(), result.getRewards());
       TestUtils.checkNotNullFields(result, "counters","residualBudget", "secondFactor","splitPayment",
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
             assertEquals(CommonUtilities.euroToCents(responseDTO.getReward().getAccruedReward()), result.getReward());
             assertEquals(responseDTO.getInitiativeId(), result.getInitiativeId());
             assertEquals(Collections.emptyList(), result.getRejectionReasons());
             assertEquals(responseDTO.getStatus(), result.getStatus());
             assertEquals(transaction.getTrxCode(), result.getTrxCode());
             assertEquals(Map.of(responseDTO.getInitiativeId(), responseDTO.getReward()), result.getRewards());
             assertEquals(responseDTO.getReward().getCounters(), result.getCounters());
             TestUtils.checkNotNullFields(result, "residualBudget", "secondFactor","splitPayment",
                     "residualAmountCents");
         });
     }

}
