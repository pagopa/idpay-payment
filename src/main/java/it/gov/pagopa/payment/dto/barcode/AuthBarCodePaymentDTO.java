package it.gov.pagopa.payment.dto.barcode;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthBarCodePaymentDTO {

    @NotNull
    private Long amountCents;
    private String idTrxAcquirer;

    private Map<String, String> additionalProperties;
}
