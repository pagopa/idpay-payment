package it.gov.pagopa.payment.dto.qrcode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionCreated {

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

}
