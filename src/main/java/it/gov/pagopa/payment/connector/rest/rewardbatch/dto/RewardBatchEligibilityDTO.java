package it.gov.pagopa.payment.connector.rest.rewardbatch.dto;

import lombok.Data;

@Data
public class RewardBatchEligibilityDTO {
    private String transactionId;
    private String initiativeId;
    private String merchantId;
    private String rewardBatchId;
    private String transactionStatus;
    private String batchStatus;
    private String batchTransactionStatus;
}
