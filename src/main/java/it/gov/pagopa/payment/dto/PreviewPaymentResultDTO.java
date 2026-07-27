package it.gov.pagopa.payment.dto;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@With
public class PreviewPaymentResultDTO {

    private String trxCode;
    private LocalDateTime trxDate;
    private SyncTrxStatus status;
    private Long originalAmountCents;
    private Long rewardCents;
    private Long residualAmountCents;
    private String userId;
    private Map<String, String> additionalProperties;
    private boolean extendedAuthorization;
}
