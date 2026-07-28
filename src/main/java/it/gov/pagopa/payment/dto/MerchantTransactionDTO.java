package it.gov.pagopa.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.gov.pagopa.payment.enums.RewardBatchTrxStatus;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.InvoiceData;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MerchantTransactionDTO {
    private String trxCode;
    private String trxId;
    private String fiscalCode;
    @NotNull
    private Long effectiveAmountCents;
    private Long rewardAmountCents;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime trxDate;
    private Long trxExpirationSeconds;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime updateDate;
    private SyncTrxStatus status;
    private Boolean splitPayment;
    private Long residualAmountCents;
    private String channel;
    private String qrcodePngUrl;
    private String qrcodeTxtUrl;
    private Map<String, String> additionalProperties;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime elaborationDateTime;
    private String pointOfSaleId;

    private OffsetDateTime trxChargeDate;
    private Long authorizedAmountCents;
    private InvoiceData invoiceData;
    private RewardBatchTrxStatus rewardBatchTrxStatus;
    private List<ReasonDTO> rewardBatchRejectionReason;
    private ChecksErrorDTO checksError;
    private String franchiseName;
}
