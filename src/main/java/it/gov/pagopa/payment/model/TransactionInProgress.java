package it.gov.pagopa.payment.model;

import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldNameConstants()
@Document(collection = "transaction_in_progress")
public class TransactionInProgress {

  @Id
  private String id;
  private String trxCode;
  private String idTrxAcquirer;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime trxDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime trxChargeDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime authDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime elaborationDateTime;

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
  private String merchantFiscalCode;
  private String vat;
  private String initiativeId;
  private Long reward;
  @Builder.Default
  private List<String> rejectionReasons = new ArrayList<>();
  private String userId;
  private SyncTrxStatus status;
  private String callbackUrl;
  private String channel;
}
