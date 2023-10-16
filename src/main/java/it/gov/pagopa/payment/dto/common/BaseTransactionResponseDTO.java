package it.gov.pagopa.payment.dto.common;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class BaseTransactionResponseDTO {

    @NotBlank
    private String id;
    @NotBlank
    private String trxCode;
    @NotBlank
    private String initiativeId;
    @NotBlank
    private String merchantId;
    @NotBlank
    private String idTrxIssuer;
    @NotBlank
    private String idTrxAcquirer;
    @NotNull
    private OffsetDateTime trxDate;
    @NotNull
    private Long amountCents;
    @NotBlank
    private String amountCurrency;
    @NotBlank
    private String mcc;
    @NotBlank
    private String acquirerId;
    @NotNull
    private SyncTrxStatus status;
    private String trxPngUrl;
    private String trxTxtUrl;
    private String merchantFiscalCode;
    private String vat;
    private Boolean splitPayment;
    private Long residualAmountCents;
    private Integer trxExpirationMinutes;
}

