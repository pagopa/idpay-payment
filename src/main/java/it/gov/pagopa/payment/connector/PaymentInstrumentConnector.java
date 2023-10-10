package it.gov.pagopa.payment.connector;

import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.dto.VerifyPinBlockDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public interface PaymentInstrumentConnector {
    VerifyPinBlockDTO checkPinBlock(@RequestBody PinBlockDTO pinBlockDTO, String userId);
}
