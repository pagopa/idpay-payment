package it.gov.pagopa.payment.connector.rest.paymentinstrument;

import feign.FeignException;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.dto.DetailsDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PaymentInstrumentConnectorImpl implements PaymentInstrumentConnector {

    private final PaymentInstrumentRestClient restClient;

    public PaymentInstrumentConnectorImpl(PaymentInstrumentRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public DetailsDTO getSecondFactor(String initiativeId, String userId){
        try{
            return restClient.getSecondFactor(initiativeId, userId); //TODO IDP-1926 check respponse
        } catch (FeignException e){ //TODO IDP-1926 check error from request
            if (e.status() == 404) {
                throw new ClientExceptionWithBody(HttpStatus.NOT_FOUND,
                        "PAYMENT_INSTRUMENT",
                        String.format("Payment instrument related to the user %s with initiativeId %s was not found.", userId, initiativeId));
            }

            throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred in the microservice payment-instrument", e);
        }
    }

}
