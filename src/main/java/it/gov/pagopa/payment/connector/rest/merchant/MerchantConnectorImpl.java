package it.gov.pagopa.payment.connector.rest.merchant;

import feign.FeignException;
import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.exception.custom.notfound.MerchantNotFoundException;
import it.gov.pagopa.payment.exception.custom.servererror.MerchantInvocationException;
import org.springframework.stereotype.Service;

@Service
public class MerchantConnectorImpl implements MerchantConnector{

    private final MerchantRestClient restClient;

    public MerchantConnectorImpl(MerchantRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public MerchantDetailDTO merchantDetail(String merchantId, String initiativeId) {
        MerchantDetailDTO merchantDetailDTO;
        try {
            merchantDetailDTO = restClient.merchantDetail(merchantId, initiativeId);
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new MerchantNotFoundException(String.format("The merchant is not related with initiative [%s]", initiativeId));
            }

            throw new MerchantInvocationException(
                    "An error occurred in the microservice merchant", false, e);
        }
        return merchantDetailDTO;
    }
}
