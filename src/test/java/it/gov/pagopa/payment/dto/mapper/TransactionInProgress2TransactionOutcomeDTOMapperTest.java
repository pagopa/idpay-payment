package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.connector.event.trx.dto.TransactionOutcomeDTO;
import it.gov.pagopa.payment.connector.event.trx.dto.mapper.TransactionInProgress2TransactionOutcomeDTOMapper;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.utils.TestUtils;
import it.gov.pagopa.payment.utils.RewardConstants;
import it.gov.pagopa.payment.utils.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class TransactionInProgress2TransactionOutcomeDTOMapperTest {

  private final TransactionInProgress2TransactionOutcomeDTOMapper mapper =
      new TransactionInProgress2TransactionOutcomeDTOMapper();

  private TransactionInProgress buildTrx() {
    TransactionInProgress trx =
        TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED);
    trx.setUserId("userId");
    trx.setAuthDate(LocalDateTime.now());
    trx.setElaborationDateTime(LocalDateTime.now());
    return trx;
  }

  @Test
  void testNoRejections() {
    // Given
    TransactionInProgress trx = buildTrx();
    trx.setReward(100L);
    trx.setRewards(
        Map.of(
            trx.getInitiativeId(),
            new Reward(
                trx.getInitiativeId(), "ORGANIZATIONID", Utils.centsToEuro(trx.getReward()))));

    // When
    TransactionOutcomeDTO result = mapper.apply(trx);

    // Then
    Assertions.assertNotNull(result);
    commonAssertions(trx, result);
    Assertions.assertEquals(trx.getRejectionReasons(), result.getRejectionReasons());
    Assertions.assertEquals(Collections.emptyMap(), result.getInitiativeRejectionReasons());
  }

  @Test
  void testOnlyInitiativeRejections() {
    // Given
    TransactionInProgress trx = buildTrx();
    trx.setReward(0L);
    trx.setRewards(Collections.emptyMap());
    trx.setRejectionReasons(List.of("INITIATIVEREJECTIONREASON"));

    // When
    TransactionOutcomeDTO result = mapper.apply(trx);

    // Then
    Assertions.assertNotNull(result);
    commonAssertions(trx, result);
    Assertions.assertEquals(Collections.emptyList(), result.getRejectionReasons());
    Assertions.assertEquals(
        Map.of(trx.getInitiativeId(), trx.getRejectionReasons()),
        result.getInitiativeRejectionReasons());
  }

  @Test
  void testNoInitiatives() {
    // Given
    TransactionInProgress trx = buildTrx();
    trx.setReward(0L);
    trx.setRewards(Collections.emptyMap());
    trx.setRejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE));

    // When
    TransactionOutcomeDTO result = mapper.apply(trx);

    // Then
    Assertions.assertNotNull(result);
    commonAssertions(trx, result);
    Assertions.assertEquals(trx.getRejectionReasons(), result.getRejectionReasons());
    Assertions.assertEquals(
        Map.of(trx.getInitiativeId(), trx.getRejectionReasons()),
        result.getInitiativeRejectionReasons());
  }

  @Test
  void testNoBudget() {
    // Given
    TransactionInProgress trx = buildTrx();
    trx.setReward(0L);
    trx.setRewards(Collections.emptyMap());
    trx.setRejectionReasons(
        List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE, "NO_BUDGET"));

    // When
    TransactionOutcomeDTO result = mapper.apply(trx);

    // Then
    Assertions.assertNotNull(result);
    commonAssertions(trx, result);
    Assertions.assertEquals(
        List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE), result.getRejectionReasons());
    Assertions.assertEquals(
        Map.of(trx.getInitiativeId(), trx.getRejectionReasons()),
        result.getInitiativeRejectionReasons());
  }

  private void commonAssertions(TransactionInProgress expected, TransactionOutcomeDTO result) {
    Assertions.assertSame(expected.getId(), result.getId());
    Assertions.assertSame(expected.getTrxCode(), result.getTrxCode());
    Assertions.assertSame(expected.getIdTrxAcquirer(), result.getIdTrxAcquirer());
    Assertions.assertSame(expected.getTrxDate(), result.getTrxDate());
    Assertions.assertSame(expected.getTrxChargeDate(), result.getTrxChargeDate());
    Assertions.assertSame(expected.getAuthDate(), result.getAuthDate());
    Assertions.assertEquals(
        (expected.getStatus().equals(SyncTrxStatus.AUTHORIZED))
            ? expected.getAuthDate()
            : expected.getElaborationDateTime(),
        result.getElaborationDateTime());
    Assertions.assertSame(expected.getOperationType(), result.getOperationType());
    Assertions.assertSame(
        expected.getOperationTypeTranscoded(), result.getOperationTypeTranscoded());
    Assertions.assertSame(expected.getIdTrxIssuer(), result.getIdTrxIssuer());
    Assertions.assertSame(expected.getCorrelationId(), result.getCorrelationId());
    Assertions.assertSame(expected.getAmountCents(), result.getAmountCents());
    Assertions.assertSame(expected.getEffectiveAmount(), result.getEffectiveAmount());
    Assertions.assertSame(expected.getAmountCurrency(), result.getAmountCurrency());
    Assertions.assertSame(expected.getMcc(), result.getMcc());
    Assertions.assertSame(expected.getAcquirerId(), result.getAcquirerId());
    Assertions.assertSame(expected.getMerchantId(), result.getMerchantId());
    Assertions.assertSame(expected.getMerchantFiscalCode(), result.getMerchantFiscalCode());
    Assertions.assertSame(expected.getVat(), result.getVat());
    Assertions.assertSame(expected.getInitiativeId(), result.getInitiativeId());
    Assertions.assertSame(expected.getUserId(), result.getUserId());
    Assertions.assertSame(expected.getStatus(), result.getStatus());
    Assertions.assertSame(expected.getChannel(), result.getChannel());
    Assertions.assertSame(expected.getRewards(), result.getRewards());
    TestUtils.checkNotNullFields(result, TransactionInProgress.Fields.reward);
  }
}
