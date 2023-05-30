package it.gov.pagopa.payment.connector.rest.merchant;

import feign.FeignException;
import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.exception.ClientExceptionWithBody;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MerchantConnectorImpl implements MerchantConnector{

    private final MerchantRestClient restClient;

    public MerchantConnectorImpl(MerchantRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public MerchantDetailDTO merchantDetail(String merchantId, String initiativeId) {
        try {
            restClient.merchantDetail(merchantId, initiativeId);
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new ClientExceptionWithBody(HttpStatus.UNAUTHORIZED,
                        "MERCHANT",
                        "resrwe");//TODO
            }
            throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred in the microservice merchant", e);
        }

        return null;
    }
}
