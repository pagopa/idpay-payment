package it.gov.pagopa.payment.dto.qrcode;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
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
  @NotBlank
  private String idTrxAcquirer;
  @NotNull
  private Long amountCents;

  private String mcc;

  private Map<String, String> additionalProperties;
}
