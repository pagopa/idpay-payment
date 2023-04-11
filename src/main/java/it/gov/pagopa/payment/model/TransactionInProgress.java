package it.gov.pagopa.payment.model;

import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.Status;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
@FieldNameConstants
@Document(collection = "transaction_in_progress")
public class TransactionInProgress {

  @Id
  private String id;
  private String trxCode;
  private String idTrxAcquirer;
  private String acquirerCode;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime trxDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime trxChargeDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime authDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
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
  private Status status;
  private String callbackUrl;
}
