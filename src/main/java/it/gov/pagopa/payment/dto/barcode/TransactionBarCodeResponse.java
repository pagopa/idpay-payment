package it.gov.pagopa.payment.dto.barcode;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionBarCodeResponse {

    private String id;
    private String trxCode;
    private String initiativeId;
    private String initiativeName;
    private OffsetDateTime trxDate;
    private SyncTrxStatus status;
    private Integer trxExpirationMinutes;
    private Long residualBudgetCents;
}
