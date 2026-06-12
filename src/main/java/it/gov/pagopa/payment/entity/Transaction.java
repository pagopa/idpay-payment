package it.gov.pagopa.payment.entity;

import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
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

    @Column(name = "trx_code", nullable = false, length = 64)
    private String trxCode;

    @Column(name = "operation_type", nullable = false, length = 32)
    private String operationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type_transcoded", length = 32)
    private OperationType operationTypeTranscoded;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SyncTrxStatus status;

    @Column(name = "trx_date", nullable = false)
    private OffsetDateTime trxDate;

    @Column(name = "trx_charge_date")
    private OffsetDateTime trxChargeDate;

    @Column(name = "elaboration_date")
    private LocalDateTime elaborationDate;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "merchant_id", length = 64)
    private String merchantId;

    @Column(name = "acquirer_id", length = 64)
    private String acquirerId;

    @Column(name = "point_of_sale_id", length = 64)
    private String pointOfSaleId;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "effective_amount_cents")
    private Long effectiveAmountCents;

    @Column(name = "voucher_amount_cents")
    private Long voucherAmountCents;

    @Column(name = "amount_currency", length = 8)
    private String amountCurrency;

    @Column(name = "channel", length = 32)
    private String channel;

    @Column(name = "initiative_id", length = 64)
    private String initiativeId;

    @Column(name = "initiative_name", length = 255)
    private String initiativeName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "initiatives", columnDefinition = "jsonb")
    private List<String> initiatives;

    @Column(name = "business_name", length = 255)
    private String businessName;

    @Column(name = "franchise_name", length = 255)
    private String franchiseName;

    @Column(name = "invoice_filename", length = 255)
    private String invoiceFilename;

    @Column(name = "invoice_doc_number", length = 255)
    private String invoiceDocNumber;

    @Column(name = "credit_note_filename", length = 255)
    private String creditNoteFilename;

    @Column(name = "credit_note_doc_number", length = 255)
    private String creditNoteDocNumber;

    @Column(name = "correlation_id", length = 128)
    private String correlationId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "reward_batch_status_trx", length = 64)
    private String rewardBatchStatusTrx;

    @Column(name = "reward_batch_id", length = 64)
    private String rewardBatchId;

    @Column(name= "product_gtin", length = 64)
    private String productGtin;

    @Column(name= "product_name", length = 64)
    private String productName;

    @Column(name = "id_trx_acquirer", length = 64)
    private String idTrxAcquirer;

    @Column(name = "merchant_fiscal_code", length = 64)
    private String merchantFiscalCode;

    @Column(name = "vat", length = 32)
    private String vat;

    @Column(name = "point_of_sale_type", length = 32)
    private String pointOfSaleType;

    @Column(name = "family_id", length = 64)
    private String familyId;

    @Column(name = "reward_cents")
    private Long rewardCents;

    @Column(name = "counter_version")
    private Long counterVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rewards", columnDefinition = "jsonb")
    private Map<String, Reward> rewards;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rejection_reasons", columnDefinition = "jsonb")
    private List<String> rejectionReasons;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "initiative_rejection_reasons", columnDefinition = "jsonb")
    private Map<String, List<String>> initiativeRejectionReasons = new HashMap<>();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_properties", columnDefinition = "jsonb")
    private Map<String, String> additionalProperties = new HashMap<>();

    @Column(name = "mcc", length = 32)
    private String mcc;

    @Column(name = "id_trx_issuer", length = 32)
    private String idTrxIssuer;

    @Column(name = "trx_end_date")
    private OffsetDateTime trxEndDate;

    @Column(name= "extended_authorization")
    private Boolean extendedAuthorization;
}