package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.connector.rest.register.dto.ProductDTO;

public interface PaymentCheckService {

    ProductDTO validateProduct(String productGtin);

}
