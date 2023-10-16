package it.gov.pagopa.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PinBlockDTO {
    @NotBlank
    private String pinBlock;
    @NotBlank
    private String encryptedKey;
}
