package it.gov.pagopa.payment.connector.rest.paymentinstrument;

import feign.FeignException;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.dto.VerifyPinBlockDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
@Slf4j
@Service
public class PaymentInstrumentRestConnectorImpl implements PaymentInstrumentRestConnector {
    private final PaymentInstrumentRestClient paymentInstrumentRestClient;

    public PaymentInstrumentRestConnectorImpl(PaymentInstrumentRestClient paymentInstrumentRestClient) {
        this.paymentInstrumentRestClient = paymentInstrumentRestClient;
    }

    @Override
    public VerifyPinBlockDTO checkPinBlock(PinBlockDTO pinBlockDTO, String userId) {
        VerifyPinBlockDTO verifyPinBlockDTO;
        try {
            verifyPinBlockDTO =  paymentInstrumentRestClient.verifyPinBlock(pinBlockDTO,userId);
        }catch (FeignException e){
            if(e.status()== 403){
                throw new ClientExceptionWithBody(HttpStatus.FORBIDDEN, PaymentConstants.ExceptionCode.INVALID_PIN,
                        "The Pinblock is incorrect");
            }
            throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred in the microservice payment-instrument", e);
        }
        log.info("[VERIFY_PIN_BLOCK] The PinBlock has been verified, for the user with userId {}",userId);
        return verifyPinBlockDTO;
    }
}
