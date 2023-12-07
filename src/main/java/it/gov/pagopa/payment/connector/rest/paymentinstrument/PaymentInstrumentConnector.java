package it.gov.pagopa.payment.connector.rest.paymentinstrument;

import it.gov.pagopa.payment.connector.rest.paymentinstrument.dto.SecondFactorDTO;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.dto.VerifyPinBlockDTO;

public interface PaymentInstrumentConnector {

    SecondFactorDTO getSecondFactor(String userId);

    VerifyPinBlockDTO checkPinBlock(PinBlockDTO pinBlockDTO, String userId);

}
