package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.connector.rest.register.dto.ProductListDTO;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductRequestDTO;

public interface PaymentCheckService {

    ProductListDTO validateProduct(ProductRequestDTO productRequestDTO);

}
