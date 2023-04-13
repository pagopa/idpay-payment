package it.gov.pagopa.payment.connector.rest.reward;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.connector.rest.reward.mapper.AuthPaymentMapper;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.AuthPaymentResponseDTOFaker;
import it.gov.pagopa.payment.test.fakers.RewardFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.utils.TestUtils;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

 class AuthPaymentMapperTest {

  private AuthPaymentMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new AuthPaymentMapper();
  }

  @Test
  void rewardMap() {

    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1,
        SyncTrxStatus.IDENTIFIED);
    transaction.setUserId("USERID%d".formatted(1));
    transaction.setHpan("HPAN%d".formatted(1));
    AuthPaymentRequestDTO result = mapper.rewardMap(transaction);

    assertAll(() -> {
      assertNotNull(result);
      assertEquals(transaction.getId(), result.getTransactionId());
      assertEquals(transaction.getUserId(), result.getUserId());
      assertEquals(transaction.getMerchantId(), result.getMerchantId());
      assertEquals(transaction.getSenderCode(), result.getSenderCode());
      assertEquals(transaction.getMerchantFiscalCode(), result.getMerchantFiscalCode());
      assertEquals(transaction.getVat(), result.getVat());
      assertEquals(transaction.getIdTrxIssuer(), result.getIdTrxIssuer());
      assertEquals(transaction.getTrxDate(), result.getTrxDate());
      assertEquals(transaction.getAmountCents(), result.getAmountCents());
      assertEquals(transaction.getAmountCurrency(), result.getAmountCurrency());
      assertEquals(transaction.getMcc(), result.getMcc());
      assertEquals(transaction.getAcquirerCode(), result.getAcquirerCode());
      assertEquals(transaction.getAcquirerId(), result.getAcquirerId());
      assertEquals(transaction.getIdTrxAcquirer(), result.getIdTrxAcquirer());
      assertEquals(transaction.getOperationTypeTranscoded(), result.getOperationType());
      assertEquals(transaction.getTrxChargeDate(), result.getTrxChargeDate());
      assertEquals(transaction.getCorrelationId(), result.getCorrelationId());
      assertEquals(transaction.getHpan(), result.getHpan());
      TestUtils.checkNotNullFields(result);
    });
  }

  @Test
  void rewardResponseMap() {
    AuthPaymentResponseDTO responseDTO = AuthPaymentResponseDTOFaker.mockInstance(1,
        SyncTrxStatus.IDENTIFIED);
    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1,
        SyncTrxStatus.IDENTIFIED);
    transaction.setRejectionReasons(List.of());

    AuthPaymentDTO result = mapper.rewardResponseMap(responseDTO, transaction);
    assertAll(() -> {
      assertNotNull(result);
      assertEquals(responseDTO.getTransactionId(), result.getId());
      assertEquals(responseDTO.getReward(), result.getReward());
      assertEquals(responseDTO.getInitiativeId(), result.getInitiativeId());
      assertEquals(responseDTO.getRejectionReasons(), result.getRejectionReasons());
      assertEquals(responseDTO.getStatus(), result.getStatus());
      assertEquals(transaction.getTrxCode(), result.getTrxCode());
      TestUtils.checkNotNullFields(result);
    });
  }

  @Test
  void trxIdempotence(){
    TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1,
        SyncTrxStatus.AUTHORIZED);
    Reward reward = RewardFaker.mockInstance(1);
    transaction.setReward(reward);
    AuthPaymentDTO result = mapper.transactionMapper(transaction);
    assertAll(() -> {
      assertNotNull(result);
      assertEquals(transaction.getId(), result.getId());
      assertEquals(transaction.getReward(), result.getReward());
      assertEquals(transaction.getInitiativeId(), result.getInitiativeId());
      assertEquals(transaction.getRejectionReasons(), result.getRejectionReasons());
      assertEquals(transaction.getStatus(), result.getStatus());
      assertEquals(transaction.getTrxCode(), result.getTrxCode());
      TestUtils.checkNotNullFields(result);
    });

  }
}
