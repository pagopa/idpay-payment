package it.gov.pagopa.payment.dto.qrcode;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionResponse {

  private String id;
  private String trxCode;
  private String initiativeId;
  private String senderCode;
  private String merchantId;
  private String idTrxIssuer;
  private String idTrxAcquirer;
  private OffsetDateTime trxDate;
  private Long amountCents;
  private String amountCurrency;
  private String mcc;
  private String acquirerCode;
  private String acquirerId;
  private SyncTrxStatus status;

}
