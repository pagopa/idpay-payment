package it.gov.pagopa.payment.connector.rest.reward.dto;

import it.gov.pagopa.payment.enums.OperationType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;

@Builder
@Getter
public class AuthPaymentRequestDTO {

  private String transactionId;
  private String userId;
  private String merchantId;
  private String senderCode;
  private String merchantFiscalCode;
  private String vat;
  private String idTrxIssuer;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime trxDate;

  private Long amountCents;
  private String amountCurrency;
  private String mcc;
  private String acquirerCode;
  private String acquirerId;
  private String idTrxAcquirer;
  private String correlationId;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime trxChargeDate;

  private OperationType operationType;
  private String hpan;
}
