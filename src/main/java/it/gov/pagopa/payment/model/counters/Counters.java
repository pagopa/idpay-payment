package it.gov.pagopa.payment.model.counters;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class Counters {
    @Builder.Default
    private Long trxNumber = 0L;
    @Builder.Default
    private BigDecimal totalReward = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
}
