package it.gov.pagopa.payment.connector.rest.reward.dto;

import it.gov.pagopa.payment.enums.OperationType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

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
  private LocalDateTime trxDate;
  private LocalDateTime trxChargeDate;
  private Long amountCents;
  private String amountCurrency;
  private String mcc;
  private String acquirerCode;
  private String acquirerId;
  private String idTrxAcquirer;
  private OperationType operationType;
  private String correlationId;

}
