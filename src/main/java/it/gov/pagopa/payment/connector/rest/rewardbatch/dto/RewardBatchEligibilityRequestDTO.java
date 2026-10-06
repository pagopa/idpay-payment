package it.gov.pagopa.payment.connector.rest.rewardbatch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RewardBatchEligibilityRequestDTO {
    private RewardBatchEligibilityOperation operation;
}
