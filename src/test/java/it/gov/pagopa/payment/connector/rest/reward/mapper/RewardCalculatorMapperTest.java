package it.gov.pagopa.payment.connector.rest.reward.mapper;

import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.AuthPaymentResponseDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

}
