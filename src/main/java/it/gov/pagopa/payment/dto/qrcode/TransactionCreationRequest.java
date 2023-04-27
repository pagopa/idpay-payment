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

  @NotBlank
  private String initiativeId;
  private String merchantFiscalCode;
  private String vat;
  private String idTrxIssuer;
  private String idTrxAcquirer;
  @NotNull
  private OffsetDateTime trxDate;
  @NotNull
  private Long amountCents;
  private String mcc;
  private String callbackUrl;

}
