package it.gov.pagopa.payment.connector.rest.paymentinstrument;

import feign.FeignException;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.dto.SecondFactorDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
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
                throw new ClientExceptionWithBody(HttpStatus.NOT_FOUND,
                        "PAYMENT_INSTRUMENT",
                        String.format("There is not a idpaycode for the userId %s.", userId));
            }

            throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred in the microservice payment-instrument.", e);
        }
    }

}
