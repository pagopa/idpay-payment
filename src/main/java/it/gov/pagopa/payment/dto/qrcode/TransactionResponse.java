package it.gov.pagopa.payment.dto.qrcode;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionResponse {

  private String id;
  private String trxCode;
  private String initiativeId;
  private String merchantId;
  private String idTrxIssuer;
  private String idTrxAcquirer;
  private OffsetDateTime trxDate;
  private Long amountCents;
  private String amountCurrency;
  private String mcc;
  private String acquirerId;
  private SyncTrxStatus status;
  private String merchantFiscalCode;
  private String vat;

}
