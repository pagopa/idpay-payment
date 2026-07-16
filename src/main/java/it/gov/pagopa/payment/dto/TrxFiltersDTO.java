package it.gov.pagopa.payment.dto;

import it.gov.pagopa.payment.enums.RewardBatchTrxStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrxFiltersDTO {
    private String status;
    private String productGtin;
    private String trxCode;
    private String merchantId;
    private String initiativeId;
    private String fiscalCode;
    private String userId;
    private String rewardBatchId;
    private RewardBatchTrxStatus rewardBatchTrxStatus;
    private String pointOfSaleId;
    private boolean includeToCheckWithConsultable;
}
