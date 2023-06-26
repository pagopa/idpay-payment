package it.gov.pagopa.payment.model;

import it.gov.pagopa.payment.enums.InitiativeRewardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InitiativeConfig {

    private String initiativeId;
    private String initiativeName;
    private String organizationId;
    private LocalDate startDate;
    private LocalDate endDate;
    private InitiativeRewardType initiativeRewardType;

}
