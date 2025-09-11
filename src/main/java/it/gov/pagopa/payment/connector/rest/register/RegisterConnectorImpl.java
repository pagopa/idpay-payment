package it.gov.pagopa.payment.connector.rest.register;

import feign.FeignException;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductCategories;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductListDTO;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductStatus;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.exception.custom.ProductInvocationException;
import it.gov.pagopa.payment.exception.custom.ProductNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class RegisterConnectorImpl implements RegisterConnector {

    private final RegisterRestClient restClient;

    public RegisterConnectorImpl(RegisterRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public ProductListDTO getProductList(String role, String organizationId, String productName, String productFileId,
                                         String eprelCode, String gtinCode, ProductStatus status, ProductCategories category, Pageable pageable) {
        ProductListDTO productListDTO;
        try {
            productListDTO = restClient.getProductList(role, organizationId, productName, productFileId, eprelCode, gtinCode, status, category, pageable);
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new ProductNotFoundException(ExceptionCode.PRODUCT_NOT_FOUND,
                        String.format("The product with gtin [%s] is not found", gtinCode),true,e);
            }

            throw new ProductInvocationException(
                    "An error occurred in the microservice payment", true, e);
        }
        return productListDTO;
    }
}
