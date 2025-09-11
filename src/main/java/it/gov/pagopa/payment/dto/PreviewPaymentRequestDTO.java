package it.gov.pagopa.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@With
public class PreviewPaymentRequestDTO {

  @NotBlank(message = "productName must not be blank")
  @JsonProperty("productName")
  private String productName;

  @NotBlank(message = "productGtin must not be blank")
  @JsonProperty("productGtin")
  private String productGtin;

  @NotNull(message = "amountCents must not be blank")
  @JsonProperty("amountCents")
  private BigDecimal amountCents;

  private Map<String, String> additionalProperties;

}
