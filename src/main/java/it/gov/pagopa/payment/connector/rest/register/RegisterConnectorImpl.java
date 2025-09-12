package it.gov.pagopa.payment.connector.rest.register;

import feign.FeignException;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductListDTO;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductStatus;
import it.gov.pagopa.payment.exception.custom.ProductInvocationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
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
            log.info("[REGISTER_CONNECTOR] Successfully retrieved products from register ProductGtin: {} and ProductStatus: {}, ReturnedCount: {}",productGtin,status, productListDTO != null ? productListDTO.getContent().size() : 0);
        } catch (FeignException e) {
            log.error("[REGISTER_CONNECTOR] Error calling register service ProductGtin: {} and ProductStatus: {}, message: {}",productGtin, status, e.getMessage());
            throw new ProductInvocationException(
                    "An error occurred in the microservice payment", true, e);
        }
        return productListDTO;
    }
}
