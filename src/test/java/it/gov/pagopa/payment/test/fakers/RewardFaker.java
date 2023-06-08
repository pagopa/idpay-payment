package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.model.counters.RewardCounters;

import java.math.BigDecimal;

public class RewardFaker {

    private RewardFaker() {
    }

    public static Reward mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    public static Reward.RewardBuilder mockInstanceBuilder(Integer bias) {
        return Reward.builder()
                .accruedReward(BigDecimal.TEN)
                .capped(true)
                .completeRefund(true)
                .initiativeId("INITIATIVEID%d".formatted(bias))
                .counters(new RewardCounters());
    }
}
