package it.gov.pagopa.payment.dto.brcode;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionBarCodeCreationRequest {

    @NotBlank
    private String initiativeId;
}
