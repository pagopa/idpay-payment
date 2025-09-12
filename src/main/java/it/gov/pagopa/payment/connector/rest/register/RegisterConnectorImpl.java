package it.gov.pagopa.payment.connector.rest.register;

import feign.FeignException;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductListDTO;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductStatus;
import it.gov.pagopa.payment.exception.custom.ProductInvocationException;
import org.springframework.stereotype.Service;

@Service
public class RegisterConnectorImpl implements RegisterConnector {

    private final RegisterRestClient restClient;

    public RegisterConnectorImpl(RegisterRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public ProductListDTO getProductList(String productGtin, ProductStatus status) {
        ProductListDTO productListDTO;
        try {
            productListDTO = restClient.getProductList(productGtin, status);
        } catch (FeignException e) {
            throw new ProductInvocationException(
                    "An error occurred in the microservice payment", true, e);
        }
        return productListDTO;
    }
}
