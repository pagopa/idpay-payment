package it.gov.pagopa.payment.dto.qrcode;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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

  @NotBlank(message = "This field is mandatory")
  private String initiativeId;
  private String merchantFiscalCode;
  private String vat;
  private String idTrxIssuer;
  private String idTrxAcquirer;
  @NotNull(message = "This field is mandatory")
  private OffsetDateTime trxDate;
  @NotNull(message = "This field is mandatory")
  private Long amountCents;
  private String mcc;
  private String callbackUrl;

}
