package it.gov.pagopa.payment.connector.rest.paymentinstrument;

import feign.FeignException;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.dto.SecondFactorDTO;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.dto.VerifyPinBlockDTO;
import it.gov.pagopa.payment.exception.custom.forbidden.PinBlockInvalidException;
import it.gov.pagopa.payment.exception.custom.notfound.IdpaycodeNotFoundException;
import it.gov.pagopa.payment.exception.custom.servererror.PaymentInstrumentInvocationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaymentInstrumentConnectorImpl implements PaymentInstrumentConnector {

    private final PaymentInstrumentRestClient restClient;

    public PaymentInstrumentConnectorImpl(PaymentInstrumentRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public SecondFactorDTO getSecondFactor(String userId){
        try{
            return restClient.getSecondFactor(userId);
        } catch (FeignException e){
            if (e.status() == 404) {
                throw new IdpaycodeNotFoundException("There is not a IDPay Code for the current user");
            }

            throw new PaymentInstrumentInvocationException(
                    "An error occurred in the microservice payment-instrument.", false, e);
        }
    }

    @Override
    public VerifyPinBlockDTO checkPinBlock(PinBlockDTO pinBlockDTO, String userId) {
        VerifyPinBlockDTO verifyPinBlockDTO;
        try {
            verifyPinBlockDTO =  restClient.verifyPinBlock(pinBlockDTO,userId);
            if(!verifyPinBlockDTO.isPinBlockVerified()){
                throw new PinBlockInvalidException("The Pinblock is incorrect");
            }
        }catch (FeignException e){
            throw new PaymentInstrumentInvocationException(
                    "An error occurred in the microservice payment-instrument", false, e);
        }
        log.info("[VERIFY_PIN_BLOCK] The PinBlock has been verified, for the user with userId {}",userId);
        return verifyPinBlockDTO;
    }

}
