package it.gov.pagopa.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@With
public class PreviewPaymentRequestV2DTO {

  @NotNull(message = "amountCents must not be blank")
  @JsonProperty("amountCents")
  private BigDecimal amountCents;

  @JsonProperty("additionalProperties")
  private Map<String, String> additionalProperties;
}
