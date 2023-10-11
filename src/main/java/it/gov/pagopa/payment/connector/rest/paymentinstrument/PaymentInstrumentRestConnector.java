package it.gov.pagopa.payment.connector.rest.paymentinstrument;

import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.dto.VerifyPinBlockDTO;

public interface PaymentInstrumentRestConnector {
    VerifyPinBlockDTO checkPinBlock(PinBlockDTO pinBlockDTO, String userId);
}
