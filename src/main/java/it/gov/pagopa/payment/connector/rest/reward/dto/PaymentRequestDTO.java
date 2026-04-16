package it.gov.pagopa.payment.connector.rest.reward.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@SuperBuilder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequestDTO {

  private String transactionId;
  private String userId;
  private String merchantId;
  private String merchantFiscalCode;
  private String vat;
  private String idTrxIssuer;
  private Instant trxDate;
  private Instant trxChargeDate;
  private Long amountCents;
  private String amountCurrency;
  private String mcc;
  private String acquirerId;
  private String idTrxAcquirer;
  private String channel;
  private long voucherAmountCents;
}
