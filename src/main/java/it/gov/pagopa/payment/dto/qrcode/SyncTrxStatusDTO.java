package it.gov.pagopa.payment.dto.qrcode;

import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SyncTrxStatusDTO {

    @NotBlank
    private String id;
    @NotBlank
    private String idTrxIssuer;
    @NotBlank
    private String trxCode;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime trxDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime authDate;

    @NotNull
    private OperationType operationType;
    @NotNull
    private Long amountCents;
    @NotBlank
    private String amountCurrency;
    @NotBlank
    private String mcc;
    @NotBlank
    private String acquirerId;
    @NotBlank
    private String merchantId;
    @NotBlank
    private String initiativeId;
    private Long rewardCents;

    @NotNull
    @Builder.Default
    private List<String> rejectionReasons = new ArrayList<>();

    @NotNull
    private SyncTrxStatus status;
}
