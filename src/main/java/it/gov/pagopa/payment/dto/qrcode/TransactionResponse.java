package it.gov.pagopa.payment.dto.qrcode;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionResponse {

  @NotEmpty
  private String id;
  @NotEmpty
  private String trxCode;
  @NotEmpty
  private String initiativeId;
  @NotEmpty
  private String merchantId;
  @NotEmpty
  private String idTrxIssuer;
  private String idTrxAcquirer;
  @NotNull
  private OffsetDateTime trxDate;
  @NotNull
  private Long amountCents;
  private String amountCurrency;
  @NotEmpty
  private String mcc;
  @NotEmpty
  private String acquirerId;
  @NotNull
  private SyncTrxStatus status;
  private String merchantFiscalCode;
  private String vat;

}
