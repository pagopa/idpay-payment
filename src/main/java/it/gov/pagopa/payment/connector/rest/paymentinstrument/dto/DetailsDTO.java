package it.gov.pagopa.payment.connector.rest.paymentinstrument.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetailsDTO {//TODO IDP-1926 check really response
    String secondFactor;
}
