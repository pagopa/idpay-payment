package it.gov.pagopa.payment.dto.qrcode;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionCreationRequest {

  private String initiativeId;
  private String merchantFiscalCode;
  private String vat;
  private String idTrxIssuer;
  private OffsetDateTime trxDate;
  private Long amountCents;
  private String mcc;
  private String callbackUrl;

}
