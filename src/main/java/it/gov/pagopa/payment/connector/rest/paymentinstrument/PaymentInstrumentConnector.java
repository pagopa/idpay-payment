package it.gov.pagopa.payment.connector.rest.paymentinstrument;

import it.gov.pagopa.payment.connector.rest.paymentinstrument.dto.SecondFactorDTO;

public interface PaymentInstrumentConnector {

    SecondFactorDTO getSecondFactor(String userId);

}
