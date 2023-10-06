package it.gov.pagopa.payment.dto.brcode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.codehaus.commons.nullanalysis.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthBarCodePaymentDTO {

    @NotNull
    private long amountCents;
}
