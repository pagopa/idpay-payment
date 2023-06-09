package it.gov.pagopa.payment.dto;

import it.gov.pagopa.payment.model.counters.RewardCounters;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Reward {
    private String initiativeId;
    private String organizationId;

    /** The ruleEngine reward calculated */
    private BigDecimal providedReward;
    /** The effective reward after CAP and REFUND evaluation */
    private BigDecimal accruedReward;
    /** True, if the reward has been capped due to budget threshold */
    private boolean capped;

    /** True, if the reward has been capped due to daily threshold */
    private boolean dailyCapped;
    /** True, if the reward has been capped due to monthly threshold */
    private boolean monthlyCapped;
    /** True, if the reward has been capped due to yearly threshold */
    private boolean yearlyCapped;
    /** True, if the reward has been capped due to weekly threshold */
    private boolean weeklyCapped;

    /** True if it's a refunding reward */
    private boolean refund;
    /** True if it's a complete refunding reward */
    private boolean completeRefund;

    /** Counters */
    private RewardCounters counters;

    public Reward(String initiativeId, String organizationId, BigDecimal reward){
        this(initiativeId, organizationId, reward, false);
    }
    public Reward(String initiativeId, String organizationId, BigDecimal reward, boolean refund){
        this(initiativeId, organizationId, reward, reward, false, refund);
    }

    public Reward(String initiativeId, String organizationId, BigDecimal providedReward, BigDecimal accruedReward){
        this(initiativeId, organizationId, providedReward, accruedReward, providedReward.compareTo(accruedReward)!=0, false);
    }

    public Reward(String initiativeId, String organizationId, BigDecimal providedReward, BigDecimal accruedReward, boolean capped, boolean refund){
        this.initiativeId=initiativeId;
        this.organizationId=organizationId;
        this.providedReward=providedReward;
        this.accruedReward=accruedReward;
        this.capped=capped;
        this.refund = refund;
    }
}
