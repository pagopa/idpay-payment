package it.gov.pagopa.payment.dto;

import it.gov.pagopa.payment.enums.RewardBatchTrxStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrxFiltersDTO {

    public static final List<String> PROCESSED_ALLOWED_STATUSES = List.of(
            "AUTHORIZED", "INVOICED", "REJECTED", "REWARDED", "CAPTURED"
    );

    private List<String> statuses;
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

    public String getStatus() {
        return !CollectionUtils.isEmpty(statuses) ? statuses.getFirst() : null;
    }
}
