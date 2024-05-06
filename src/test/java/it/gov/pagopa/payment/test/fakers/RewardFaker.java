package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.model.counters.RewardCounters;


public class RewardFaker {

    private RewardFaker() {
    }

    public static Reward mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    public static Reward.RewardBuilder mockInstanceBuilder(Integer bias) {
        return Reward.builder()
                .accruedRewardCents(1000L)
                .capped(true)
                .completeRefund(true)
                .initiativeId("INITIATIVEID%d".formatted(bias))
                .counters(new RewardCounters());
    }
}
