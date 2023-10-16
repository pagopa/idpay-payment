package it.gov.pagopa.payment.connector.rest.merchant;

import feign.FeignException;
import it.gov.pagopa.common.web.exception.custom.ForbiddenException;
import it.gov.pagopa.common.web.exception.custom.InternalServerErrorException;
import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
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
                throw new ForbiddenException("MERCHANT",
                        String.format("The merchant %s is not related with initiative %s", merchantId, initiativeId));
            }

            throw new InternalServerErrorException("INTERNAL SERVER ERROR",
                    "An error occurred in the microservice merchant", false, e);
        }
        return merchantDetailDTO;
    }
}
