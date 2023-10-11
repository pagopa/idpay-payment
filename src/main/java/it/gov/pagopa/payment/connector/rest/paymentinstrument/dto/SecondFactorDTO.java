package it.gov.pagopa.payment.connector.rest.paymentinstrument.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecondFactorDTO {
    private String secondFactor;
}
