package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.model.counters.RewardCounters;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class AuthPaymentDTOFaker {

  private AuthPaymentDTOFaker() {}

  public static AuthPaymentDTO mockInstance(Integer bias, TransactionInProgress transaction){
    return mockInstanceBuilder(bias,transaction).build();
  }

  public static AuthPaymentDTO.AuthPaymentDTOBuilder<?,?> mockInstanceBuilder(Integer bias, TransactionInProgress transaction) {
    Reward reward = new Reward(transaction.getInitiativeId(), "ORGID", TestUtils.bigDecimalValue(10));
    reward.setCounters(RewardCounters.builder().exhaustedBudget(false).initiativeBudget(BigDecimal.valueOf(100)).totalReward(BigDecimal.valueOf(50)).build());
    Long residualBudget = CommonUtilities.euroToCents(reward.getCounters().getInitiativeBudget().subtract(reward.getCounters().getTotalReward()));

    return AuthPaymentDTO.builder()
        .id(transaction.getId())
        .initiativeId(transaction.getInitiativeId())
        .initiativeName("INITIATIVENAME%d".formatted(bias))
        .businessName("BUSINESSNAME%d".formatted(bias))
        .status(SyncTrxStatus.IDENTIFIED)
        .rejectionReasons(List.of())
        .amountCents(10_00L)
        .reward(10_00L)
        .trxCode("trxcode%d".formatted(bias))
        .trxDate(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS))
        .rewards(Map.of(transaction.getInitiativeId(), reward))
        .counters(reward.getCounters())
        .counterVersion(1L)
        .residualBudget(residualBudget);
  }
}
