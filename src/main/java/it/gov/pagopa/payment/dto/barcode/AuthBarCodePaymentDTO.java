package it.gov.pagopa.payment.dto.barcode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthBarCodePaymentDTO {

    @NotNull
    private Long amountCents;
    @NotBlank
    private String idTrxAcquirer;
}
