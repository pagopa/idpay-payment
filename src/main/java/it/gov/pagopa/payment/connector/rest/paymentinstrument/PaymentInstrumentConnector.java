package it.gov.pagopa.payment.connector.rest.paymentinstrument;

import it.gov.pagopa.payment.connector.rest.paymentinstrument.dto.DetailsDTO;

public interface PaymentInstrumentConnector {

    DetailsDTO getSecondFactor(String initiativeId, String userId); //TODO IDP-1926 check really response

}
