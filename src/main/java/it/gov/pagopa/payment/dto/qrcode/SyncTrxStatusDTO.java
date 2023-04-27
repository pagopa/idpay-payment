package it.gov.pagopa.payment.dto.qrcode;

import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SyncTrxStatusDTO {

    @NotEmpty
    private String id;
    @NotEmpty
    private String idTrxIssuer;
    @NotEmpty
    private String trxCode;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime trxDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime authDate;

    @NotNull
    private OperationType operationType;
    @NotNull
    private Long amountCents;
    @NotEmpty
    private String amountCurrency;
    @NotEmpty
    private String mcc;
    @NotEmpty
    private String acquirerId;
    @NotEmpty
    private String merchantId;
    @NotEmpty
    private String initiativeId;
    private Long rewardCents;

    @NotNull
    @Builder.Default
    private List<String> rejectionReasons = new ArrayList<>();

    @NotNull
    private SyncTrxStatus status;
}
