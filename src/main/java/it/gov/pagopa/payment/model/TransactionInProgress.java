package it.gov.pagopa.payment.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldNameConstants
@Document(collection = "transaction_in_progress")
public class TransactionInProgress {

  @Id
  String id;
  String trxCode;
  String initiativeId;
  String userId;
  String merchantId;
  String senderCode;
  String merchantFiscalCode;
  String vat;
  String idTrxIssuer;
  String idTrxAcquirer;
  LocalDateTime trxDate;
  BigDecimal amount;
  String amountCurrency;
  String mcc;
  String acquirerCode;
  String acquirerId;
  Status status;

  private enum Status{
    CREATED
  }

}
