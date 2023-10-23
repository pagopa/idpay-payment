package it.gov.pagopa.payment.connector.rest.paymentinstrument;

import feign.FeignException;
import it.gov.pagopa.common.web.exception.custom.forbidden.PinBlockInvalidException;
import it.gov.pagopa.common.web.exception.custom.notfound.IdpaycodeNotFoundException;
import it.gov.pagopa.common.web.exception.custom.servererror.PaymentInstrumentInvocationException;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.dto.SecondFactorDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.dto.VerifyPinBlockDTO;
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
                throw new IdpaycodeNotFoundException(
                    ExceptionCode.IDPAYCODE_NOT_FOUND,
                        String.format("There is not a idpaycode for the userId %s.", userId));
            }

            throw new PaymentInstrumentInvocationException(
                    ExceptionCode.GENERIC_ERROR,
                    "An error occurred in the microservice payment-instrument.", false, e);
        }
    }

    @Override
    public VerifyPinBlockDTO checkPinBlock(PinBlockDTO pinBlockDTO, String userId) {
        VerifyPinBlockDTO verifyPinBlockDTO;
        try {
            verifyPinBlockDTO =  restClient.verifyPinBlock(pinBlockDTO,userId);
            if(!verifyPinBlockDTO.isPinBlockVerified()){
                throw new PinBlockInvalidException(
                        PaymentConstants.ExceptionCode.INVALID_PIN,
                        "The Pinblock is incorrect");
            }
        }catch (FeignException e){
            throw new PaymentInstrumentInvocationException(
                    ExceptionCode.GENERIC_ERROR,
                    "An error occurred in the microservice payment-instrument", false, e);
        }
        log.info("[VERIFY_PIN_BLOCK] The PinBlock has been verified, for the user with userId {}",userId);
        return verifyPinBlockDTO;
    }

}
