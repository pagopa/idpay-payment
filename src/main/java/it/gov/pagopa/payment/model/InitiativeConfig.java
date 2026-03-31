package it.gov.pagopa.payment.model;

import it.gov.pagopa.payment.enums.InitiativeRewardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InitiativeConfig {

    private String initiativeId;
    private String initiativeName;
    private String organizationId;
    private Instant startDate;
    private Instant endDate;
    private InitiativeRewardType initiativeRewardType;

}
