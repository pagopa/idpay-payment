package it.gov.pagopa.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PinBlockDTO {
    private String encryptedPinBlock;
    private String encryptedKey;
}
