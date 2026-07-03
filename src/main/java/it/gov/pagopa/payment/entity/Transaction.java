package it.gov.pagopa.payment.entity;

import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.InvoiceData;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "transaction")
public class Transaction {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "\"trxCode\"", nullable = false, length = 64)
    private String trxCode;

    @Column(name = "\"operationType\"", nullable = false, length = 32)
    private String operationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "\"operationTypeTranscoded\"", length = 32)
    private OperationType operationTypeTranscoded;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SyncTrxStatus status;

    @Column(name = "\"trxDate\"", nullable = false)
    private OffsetDateTime trxDate;

    @Column(name = "\"trxChargeDate\"")
    private OffsetDateTime trxChargeDate;

    @Column(name = "\"elaborationDate\"")
    private LocalDateTime elaborationDate;

    @Column(name = "\"updateDate\"")
    private LocalDateTime updateDate;

    @Column(name = "\"userId\"", length = 64)
    private String userId;

    @Column(name = "\"merchantId\"", length = 64)
    private String merchantId;

    @Column(name = "\"acquirerId\"", length = 64)
    private String acquirerId;

    @Column(name = "\"pointOfSaleId\"", length = 64)
    private String pointOfSaleId;

    @Column(name = "\"amountCents\"", nullable = false)
    private Long amountCents;

    @Column(name = "\"effectiveAmountCents\"")
    private Long effectiveAmountCents;

    @Column(name = "\"voucherAmountCents\"")
    private Long voucherAmountCents;

    @Column(name = "\"amountCurrency\"", length = 8)
    private String amountCurrency;

    @Column(name = "channel", length = 32)
    private String channel;

    @Column(name = "\"initiativeId\"", length = 64)
    private String initiativeId;

    @Column(name = "\"initiativeName\"", length = 255)
    private String initiativeName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "initiatives", columnDefinition = "jsonb")
    private List<String> initiatives;

    @Column(name = "\"businessName\"", length = 255)
    private String businessName;

    @Column(name = "\"franchiseName\"", length = 255)
    private String franchiseName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"invoiceData\"", columnDefinition = "jsonb")
    private InvoiceData invoiceData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"creditNoteData\"", columnDefinition = "jsonb")
    private InvoiceData creditNoteData;

    @Column(name = "\"correlationId\"", length = 128)
    private String correlationId;

    @Column(name = "\"createdAt\"")
    private LocalDateTime createdAt;

    @Column(name = "\"rewardBatchStatusTrx\"", length = 64)
    private String rewardBatchStatusTrx;

    @Column(name = "\"rewardBatchId\"", length = 64)
    private String rewardBatchId;

    @Column(name = "\"productGtin\"", length = 64)
    private String productGtin;

    @Column(name = "\"productName\"", length = 64)
    private String productName;

    @Column(name = "\"idTrxAcquirer\"", length = 64)
    private String idTrxAcquirer;

    @Column(name = "\"merchantFiscalCode\"", length = 64)
    private String merchantFiscalCode;

    @Column(name = "vat", length = 32)
    private String vat;

    @Column(name = "\"pointOfSaleType\"", length = 32)
    private String pointOfSaleType;

    @Column(name = "\"familyId\"", length = 64)
    private String familyId;

    @Column(name = "\"rewardCents\"")
    private Long rewardCents;

    @Column(name = "\"counterVersion\"")
    private Long counterVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rewards", columnDefinition = "jsonb")
    private Map<String, Reward> rewards;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"rejectionReasons\"", columnDefinition = "jsonb")
    private List<String> rejectionReasons;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"initiativeRejectionReasons\"", columnDefinition = "jsonb")
    private Map<String, List<String>> initiativeRejectionReasons = new HashMap<>();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"additionalProperties\"", columnDefinition = "jsonb")
    private Map<String, String> additionalProperties = new HashMap<>();

    @Column(name = "mcc", length = 32)
    private String mcc;

    @Column(name = "\"idTrxIssuer\"", length = 32)
    private String idTrxIssuer;

    @Column(name = "\"trxEndDate\"")
    private OffsetDateTime trxEndDate;

    @Column(name = "\"extendedAuthorization\"")
    private Boolean extendedAuthorization;
}