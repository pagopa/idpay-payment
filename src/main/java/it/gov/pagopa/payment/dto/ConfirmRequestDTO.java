package it.gov.pagopa.payment.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConfirmRequestDTO {

  @NotBlank
  private String trxCode;

  private boolean confirmed;

}
