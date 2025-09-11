package it.gov.pagopa.payment.connector.rest.register;

import it.gov.pagopa.payment.connector.rest.register.dto.ProductListDTO;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductRequestDTO;

public interface RegisterConnector {
    ProductListDTO getProductList(ProductRequestDTO productRequestDTO);
}
