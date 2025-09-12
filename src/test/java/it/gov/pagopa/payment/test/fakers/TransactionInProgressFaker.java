package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.model.counters.RewardCounters;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionInProgressFaker {

  public static TransactionInProgress mockInstance(Integer bias, SyncTrxStatus status) {
    return mockInstanceBuilder(bias, status).build();
  }

  public static TransactionInProgress.TransactionInProgressBuilder<?,?> mockInstanceBuilder(Integer bias, SyncTrxStatus status) {

    String id = "MOCKEDTRANSACTION_qr-code_%d".formatted(bias);
    String initiativeId = "INITIATIVEID%d".formatted(bias);

    Long rewardCents=null;
    Map<String, Reward> rewards;
    if(!status.equals(SyncTrxStatus.CREATED)){
      rewardCents=1_00L;
      Reward rewardObj = new Reward(initiativeId, "ORGID", 100L);
      rewardObj.setCounters(RewardCounters.builder().exhaustedBudget(false).initiativeBudgetCents(10000L).totalRewardCents(5000L).build());
      rewards=new HashMap<>(Map.of(initiativeId, rewardObj));
    } else {
      rewards = Collections.emptyMap();
    }

    boolean trxStatus = status.equals(SyncTrxStatus.REJECTED) || status.equals(SyncTrxStatus.REWARDED) || status.equals(SyncTrxStatus.AUTHORIZED);

    Map<String, String> additionalProperties = new HashMap<>();
    additionalProperties.put("description", "test 1234");

    return TransactionInProgress.builder()
        .id(id)
        .correlationId(id)
        .initiativeId(initiativeId)
        .initiatives(List.of(initiativeId))
        .initiativeName("INITIATIVENAME%d".formatted(bias))
        .businessName("BUSINESSNAME%d".formatted(bias))
        .merchantId("MERCHANTID%d".formatted(bias))
        .pointOfSaleId("POINTOFSALEID%d".formatted(bias))
        .merchantFiscalCode("MERCHANTFISCALCODE%d".formatted(bias))
        .vat("VAT%d".formatted(bias))
        .trxDate(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS))
        .trxChargeDate(trxStatus ? OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS) : null)
        .amountCents(1000L)
        .effectiveAmountCents(1000L)
        .amountCurrency("AMOUNTCURRENCY%d".formatted(bias))
        .mcc("MCC%d".formatted(bias))
        .acquirerId("ACQUIRERID%d".formatted(bias))
        .idTrxAcquirer("IDTRXACQUIRER%d".formatted(bias))
        .idTrxIssuer("IDTRXISSUER%d".formatted(bias))
        .trxCode("trxcode%d".formatted(bias))
        .operationType(PaymentConstants.OPERATION_TYPE_CHARGE)
        .operationTypeTranscoded(OperationType.CHARGE)
        .status(status)
        .channel("CHANNEL%d".formatted(bias))
        .rewardCents(rewardCents)
        .counterVersion(0L)
        .rewards(rewards)
        .additionalProperties(additionalProperties)
        .updateDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
  }
}
