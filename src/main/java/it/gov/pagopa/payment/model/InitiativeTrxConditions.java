package it.gov.pagopa.payment.model;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class InitiativeTrxConditions {
    private TrxCountDTO trxCount;
}