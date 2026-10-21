package it.gov.pagopa.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.payment.model.InvoiceData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PointOfSaleTransactionDTO {

    @JsonProperty("id")
    String trxId;
    String trxCode;
    String fiscalCode;
    Long effectiveAmountCents;
    Long rewardAmountCents;
    Long authorizedAmountCents;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    LocalDateTime trxDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    LocalDateTime  trxChargeDate;
    String status;
    String rewardBatchTrxStatus;
    String channel;
    Map<String, String> additionalProperties;
    @JsonProperty("invoiceFile")
    InvoiceData invoiceData;

    private Long trxExpirationSeconds;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime updateDate;
    private Boolean splitPayment;
    private Long residualAmountCents;
    private String qrcodePngUrl;
    private String qrcodeTxtUrl;
}
