package it.gov.pagopa.payment.dto.qrcode;

import it.gov.pagopa.payment.dto.common.BaseTransactionResponseDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class TransactionResponse extends BaseTransactionResponseDTO {
  @NotBlank
  private String qrcodePngUrl;
  @NotBlank
  private String qrcodeTxtUrl;
}
