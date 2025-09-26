package it.gov.pagopa.payment.dto.barcode;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class TransactionBarCodeResponse {

    private String id;
    private String trxCode;
    private String initiativeId;
    private String initiativeName;
    private OffsetDateTime trxDate;
    private SyncTrxStatus status;
    private Long trxExpirationSeconds;
    private Long residualBudgetCents;
    private OffsetDateTime trxEndDate;
    private Long voucherAmountCents;
}
