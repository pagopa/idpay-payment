package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.model.counters.RewardCounters;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthPaymentDTOFaker {

  private AuthPaymentDTOFaker() {}

  public static AuthPaymentDTO mockInstance(Integer bias, TransactionInProgress transaction){
    return mockInstanceBuilder(bias,transaction).build();
  }

  public static AuthPaymentDTO.AuthPaymentDTOBuilder<?,?> mockInstanceBuilder(Integer bias, TransactionInProgress transaction) {
    Reward reward = new Reward(transaction.getInitiativeId(), "ORGID", 1000L);
    reward.setCounters(RewardCounters.builder().exhaustedBudget(false).initiativeBudgetCents(100000L).totalRewardCents(5000L).build());
    Long residualBudget = reward.getCounters().getInitiativeBudgetCents() - reward.getCounters().getTotalRewardCents();

    Map<String, String> additionalProperties = new HashMap<>();

    additionalProperties.put("description", "Additional description");

    return AuthPaymentDTO.builder()
        .id(transaction.getId())
        .initiativeId(transaction.getInitiativeId())
        .initiativeName("INITIATIVENAME%d".formatted(bias))
        .businessName("BUSINESSNAME%d".formatted(bias))
        .status(SyncTrxStatus.IDENTIFIED)
        .rejectionReasons(List.of())
        .amountCents(10_00L)
        .rewardCents(10_00L)
        .trxCode("trxcode%d".formatted(bias))
        .trxDate(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS))
        .rewards(Map.of(transaction.getInitiativeId(), reward))
        .counters(reward.getCounters())
        .residualBudgetCents(residualBudget)
        .additionalProperties(additionalProperties)
        .counterVersion(0L);
  }
}
