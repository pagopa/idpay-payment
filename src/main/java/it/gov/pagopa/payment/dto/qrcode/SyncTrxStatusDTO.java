package it.gov.pagopa.payment.dto.qrcode;

import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SyncTrxStatusDTO {

    private String id;
    private String idTrxIssuer;
    private String trxCode;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant trxDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant authDate;

    private OperationType operationType;
    private Long amountCents;
    private String amountCurrency;
    private String mcc;
    private String acquirerId;
    private String merchantId;
    private String initiativeId;
    private Long rewardCents;

    @Builder.Default
    private List<String> rejectionReasons = new ArrayList<>();

    private SyncTrxStatus status;

    private Boolean splitPayment;
    private Long residualAmountCents;

    private String qrcodePngUrl;
    private String qrcodeTxtUrl;
}
