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
  private String senderCode;
  private String merchantFiscalCode;
  private String vat;
  private String idTrxIssuer;
  private String idTrxAcquirer;
  @NotNull
  private OffsetDateTime trxDate;
  @NotNull
  private Long amountCents;
  private String amountCurrency;
  private String mcc;
  private String acquirerCode;
  private String acquirerId;
  private String callbackUrl;

}
