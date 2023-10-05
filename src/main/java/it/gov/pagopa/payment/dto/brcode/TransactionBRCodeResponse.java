package it.gov.pagopa.payment.dto.brcode;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionBRCodeResponse {

    @NotBlank
    private String id;
    @NotBlank
    private String trxCode;
    @NotBlank
    private String initiativeId;
    @NotNull
    private OffsetDateTime trxDate;
    @NotNull
    private SyncTrxStatus status;
    private Integer trxExpirationMinutes;
}
