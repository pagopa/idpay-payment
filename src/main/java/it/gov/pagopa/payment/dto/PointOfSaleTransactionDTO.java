package it.gov.pagopa.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PointOfSaleTransactionDTO {

    private String trxCode;
    @JsonProperty("id")
    private String trxId;
    private String fiscalCode;
    @NotNull
    private Long effectiveAmountCents;
    private Long rewardAmountCents;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime trxDate;
    private Long trxExpirationSeconds;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime updateDate;
    private SyncTrxStatus status;
    private Boolean splitPayment;
    private Long residualAmountCents;
    private String channel;
    private String qrcodePngUrl;
    private String qrcodeTxtUrl;
}
