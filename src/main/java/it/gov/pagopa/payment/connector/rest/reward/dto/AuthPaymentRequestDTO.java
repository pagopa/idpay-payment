package it.gov.pagopa.payment.connector.rest.reward.dto;

import it.gov.pagopa.payment.enums.OperationType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class AuthPaymentRequestDTO {

  String transactionId;
  String userId;
  String merchantId;
  String senderCode;
  String merchantFiscalCode;
  String vat;
  String idTrxIssuer;
  LocalDateTime trxDate;
  LocalDateTime trxChargeDate;
  Long amountCents;
  String amountCurrency;
  String mcc;
  String acquirerCode;
  String acquirerId;
  String idTrxAcquirer;
  OperationType operationType;
  String correlationId;

}
