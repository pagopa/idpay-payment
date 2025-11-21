package it.gov.pagopa.payment.connector.rest.merchant;

import feign.FeignException;
import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.connector.rest.merchant.dto.PointOfSaleDTO;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.MerchantInvocationException;
import it.gov.pagopa.payment.exception.custom.PosNotFoundException;
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
                throw new MerchantOrAcquirerNotAllowedException(ExceptionCode.MERCHANT_NOT_ONBOARDED,
                        String.format("The current merchant is not related with initiative [%s]", initiativeId),true,e);
            }

            throw new MerchantInvocationException(
                    "An error occurred in the microservice merchant", true, e);
        }
        return merchantDetailDTO;
    }

    @Override
    public PointOfSaleDTO getPointOfSale(String merchantId, String pointOfSaleId) {
        try {
            return restClient.getPointOfSale(merchantId, pointOfSaleId);
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new PosNotFoundException(
                    String.format("POS with id [%s] not found for merchant [%s]", pointOfSaleId, merchantId), e
                );
            }

            throw new MerchantInvocationException(
                "An error occurred in the microservice merchant", true, e);
        }
    }
}
