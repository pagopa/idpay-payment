package it.gov.pagopa.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import java.time.Instant;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@With
public class PreviewPaymentResponseV2DTO {

    private String trxCode;
    private Instant trxDate;
    private SyncTrxStatus status;
    private Long originalAmountCents;
    private Long rewardCents;
    private Long residualAmountCents;
    private String userId;
    private Map<String, String> additionalProperties;
    private boolean extendedAuthorization;
}
