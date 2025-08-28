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

  @NotBlank(message = "product must not be blank")
  @JsonProperty("product")
  private String product;

  @NotNull(message = "amount must not be blank")
  @JsonProperty("amount")
  private BigDecimal amount;

  @NotBlank(message = "trxCode must not be blank")
  @JsonProperty("trxCode")
  private String trxCode;


}
