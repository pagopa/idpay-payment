package it.gov.pagopa.payment.connector.rest.reward.dto;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public class AuthPaymentRequestDTO {

  String transactionId;
  String userId;
  String merchantId;
  String senderCode;
  String merchantFiscalCode;
  String vat;
  String idTrxIssuer;
  LocalDateTime trxDate;
  Long amountCents;
  String amountCurrency;
  String mcc;
  String acquirerCode;
  String acquirerId;
  String idTrxAcquirer;

}
