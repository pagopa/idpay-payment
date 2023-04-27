package it.gov.pagopa.payment.dto.qrcode;

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
public class TransactionCreationRequest {

  @NotEmpty
  private String initiativeId;
  @NotEmpty
  private String merchantFiscalCode;
  @NotEmpty
  private String vat;
  @NotEmpty
  private String idTrxIssuer;
  @NotNull
  private OffsetDateTime trxDate;
  @NotNull
  private Long amountCents;
  @NotEmpty
  private String mcc;

}
