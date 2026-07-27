package it.gov.pagopa.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@With
public class PreviewPaymentDTO {

    private String trxCode;
    private LocalDateTime trxDate;
    private SyncTrxStatus status;
    private Long originalAmountCents;
    private Long rewardCents;
    private Long residualAmountCents;
    private String userId;
    private String productName;
    private String productGtin;
    private boolean extendedAuthorization;
}
