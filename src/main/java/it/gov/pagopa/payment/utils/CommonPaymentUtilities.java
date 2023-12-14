package it.gov.pagopa.payment.utils;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.model.counters.RewardCounters;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public class CommonPaymentUtilities {
    private static final Pair<Boolean, Long> PAIR_NULL = Pair.of(null,null);

    private CommonPaymentUtilities() {}

    public static Long calculateResidualBudget(Map<String, Reward> rewards) {
        Long residualBudget = null;
        Reward reward = rewards.values().stream().findFirst().orElse(null);
        RewardCounters rewardCounters = reward != null ? reward.getCounters() : null;
        if (reward != null && rewardCounters != null) {
            residualBudget = CommonUtilities.euroToCents(rewardCounters.getInitiativeBudget().subtract(rewardCounters.getTotalReward()));
        }
        return residualBudget;
    }
    public static Pair<Boolean, Long> getSplitPaymentAndResidualAmountCents(Long amountCents, Long reward) {
        if (reward != null) {
            long residualAmountCents = amountCents - reward;
            if (residualAmountCents >= 0L) {
                return Pair.of(residualAmountCents > 0L, residualAmountCents);
            }
        }
        return PAIR_NULL;
    }
}