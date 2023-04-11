package it.gov.pagopa.payment.dto.qrcode;

import java.math.BigDecimal;
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
  BigDecimal amount;
  String amountCurrency;
  String mcc;
  String acquirerCode;
  String acquirerId;
  String idTrxAcquirer;

}
