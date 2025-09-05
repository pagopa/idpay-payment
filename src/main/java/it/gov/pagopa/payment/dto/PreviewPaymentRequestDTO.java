package it.gov.pagopa.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
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

}
