package it.gov.pagopa.payment.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.model.counters.RewardCounters;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
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

    public static Map<String, List<String>> getInitiativeRejectionReason(String initiativeId, List<String> rejectionReasons){
        if(!CollectionUtils.isEmpty(rejectionReasons)){
            return Map.of(initiativeId, rejectionReasons);
        }
        return Collections.emptyMap();
    }
    public static <T> String convertToJson(T object, ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error converting request in JSON",e);
        }
    }
}
