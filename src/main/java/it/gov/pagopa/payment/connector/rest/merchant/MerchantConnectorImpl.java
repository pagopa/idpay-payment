package it.gov.pagopa.payment.connector.rest.merchant;

import feign.FeignException;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
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
        MerchantDetailDTO merchantDetailDTO;
        try {
            merchantDetailDTO = restClient.merchantDetail(merchantId, initiativeId);
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new ClientExceptionWithBody(HttpStatus.FORBIDDEN,
                        PaymentConstants.ExceptionCode.MERCHANT_NOT_FOUND,
                        String.format("The merchant is not related with initiative [%s]", initiativeId));
            }

            throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred in the microservice merchant", e);
        }
        return merchantDetailDTO;
    }
}
