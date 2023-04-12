package it.gov.pagopa.payment.dto;

import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionDTO {
    private String id;
    private String trxCode;
    private String idTrxAcquirer;
    private String acquirerCode;
    private LocalDateTime trxDate;
    private LocalDateTime trxChargeDate;
    private LocalDateTime authDate;
    private LocalDateTime elaborationDateTime;
    private String hpan;
    private String operationType;
    private OperationType operationTypeTranscoded;
    private String idTrxIssuer;
    private String correlationId;
    private Long amountCents;
    private BigDecimal effectiveAmount;
    private String amountCurrency;
    private String mcc;
    private String acquirerId;
    private String merchantId;
    private String senderCode;
    private String merchantFiscalCode;
    private String vat;
    private String initiativeId;
    private String userId;
    private SyncTrxStatus status;
    private String callbackUrl;
}
