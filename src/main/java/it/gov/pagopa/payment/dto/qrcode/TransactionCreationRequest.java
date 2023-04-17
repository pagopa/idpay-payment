package it.gov.pagopa.payment.dto.qrcode;

import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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
  private String senderCode;
  private String merchantFiscalCode;
  private String vat;
  private String idTrxIssuer;
  private String idTrxAcquirer;
  @NotNull(message = "This field is mandatory")
  private LocalDateTime trxDate;
  @NotNull(message = "This field is mandatory")
  private Long amountCents;
  private String amountCurrency;
  private String mcc;
  private String acquirerCode;
  private String acquirerId;
  private String callbackUrl;

}
