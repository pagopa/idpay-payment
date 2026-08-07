package it.gov.pagopa.payment.connector.event.trx;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.InvoiceData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RewardTransactionDTO {

    private String id;
    private String trxCode;
    private String operationType;
    private OperationType operationTypeTranscoded;
    private SyncTrxStatus status;
    private OffsetDateTime trxDate;
    private OffsetDateTime trxChargeDate;
    private LocalDateTime elaborationDateTime;
    private LocalDateTime updateDate;
    private String userId;
    private String merchantId;
    private String acquirerId;
    private String pointOfSaleId;
    private Long amountCents;
    private Long effectiveAmountCents;
    private Long voucherAmountCents;
    private String amountCurrency;
    private String channel;
    private String initiativeId;
    private String initiativeName;
    private List<String> initiatives;
    private String businessName;
    private String franchiseName;
    private InvoiceData invoiceData;
    private InvoiceData creditNoteData;
    private String correlationId;
    private LocalDateTime createdAt;
    private String idTrxAcquirer;
    private String merchantFiscalCode;
    private String vat;
    private String pointOfSaleType;
    private String productType;
    private String familyId;
    private Long rewardCents;
    private Long counterVersion;
    private Long transactionRevision;
    private Map<String, Reward> rewards;
    private List<String> rejectionReasons;
    private Map<String, List<String>> initiativeRejectionReasons;
    private Map<String, String> additionalProperties;
    private String mcc;
    private String idTrxIssuer;
    private OffsetDateTime trxEndDate;
    private Boolean extendedAuthorization;
    private String productName;
}
