package it.gov.pagopa.payment.model.counters;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class RewardCounters extends Counters {
    private boolean exhaustedBudget;
    private BigDecimal initiativeBudget;
}
