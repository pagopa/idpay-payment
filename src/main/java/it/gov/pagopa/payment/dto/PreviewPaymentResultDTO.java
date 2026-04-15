package it.gov.pagopa.payment.dto;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@With
public class PreviewPaymentResultDTO {

    private String trxCode;
    private OffsetDateTime trxDate;
    private SyncTrxStatus status;
    private Long originalAmountCents;
    private Long rewardCents;
    private Long residualAmountCents;
    private String userId;
    private Map<String, String> additionalProperties;
    private boolean extendedAuthorization;
}
