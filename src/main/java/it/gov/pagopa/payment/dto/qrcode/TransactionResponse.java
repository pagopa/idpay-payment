package it.gov.pagopa.payment.dto.qrcode;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

  @NotBlank
  private String id;
  @NotBlank
  private String trxCode;
  @NotBlank
  private String initiativeId;
  @NotBlank
  private String merchantId;
  @NotBlank
  private String idTrxIssuer;
  private String idTrxAcquirer;
  @NotNull
  private OffsetDateTime trxDate;
  @NotNull
  private Long amountCents;
  private String amountCurrency;
  @NotBlank
  private String mcc;
  @NotBlank
  private String acquirerId;
  @NotNull
  private SyncTrxStatus status;
  private String merchantFiscalCode;
  private String vat;

}
