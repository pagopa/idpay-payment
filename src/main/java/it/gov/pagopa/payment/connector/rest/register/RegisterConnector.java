package it.gov.pagopa.payment.connector.rest.register;

import it.gov.pagopa.payment.connector.rest.register.dto.ProductListDTO;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductStatus;

public interface RegisterConnector {
    ProductListDTO getProductList(String productGtin, ProductStatus status);
}
