package it.gov.pagopa.payment.dto.qrcode;

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
public class TransactionCreationRequest {

  @NotBlank
  private String initiativeId;
  @NotBlank
  private String merchantFiscalCode;
  @NotBlank
  private String vat;
  @NotBlank
  private String idTrxIssuer;
  @NotNull
  private OffsetDateTime trxDate;
  @NotNull
  private Long amountCents;
  @NotBlank
  private String mcc;

}
