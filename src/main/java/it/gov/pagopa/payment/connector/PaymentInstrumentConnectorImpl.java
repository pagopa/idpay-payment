package it.gov.pagopa.payment.connector;

import feign.FeignException;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.dto.VerifyPinBlockDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PaymentInstrumentConnectorImpl implements PaymentInstrumentConnector {
    private final PaymentInstrumentRest paymentInstrumentRest;

    public PaymentInstrumentConnectorImpl(PaymentInstrumentRest paymentInstrumentRest) {
        this.paymentInstrumentRest = paymentInstrumentRest;
    }

    @Override
    public VerifyPinBlockDTO checkPinBlock(PinBlockDTO pinBlockDTO, String userId) {
        VerifyPinBlockDTO verifyPinBlockDTO;
        try {
            verifyPinBlockDTO =  paymentInstrumentRest.verifyPinBlock(pinBlockDTO,userId);
        }catch (FeignException e){
            if(e.status()== 403){
                throw new ClientExceptionWithBody(HttpStatus.FORBIDDEN, PaymentConstants.ExceptionCode.INVALID_PIN,
                        "The Pinblock is incorrect");
            }
            throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred in the microservice payment-instrument", e);
        }
        return verifyPinBlockDTO;
    }
}
