package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.connector.rest.register.dto.ProductCategories;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductListDTO;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductStatus;
import org.springframework.data.domain.Pageable;

public interface PaymentCheckService {

    ProductListDTO validateProduct(String role, String organizationId, String productName, String productFileId,
                                String eprelCode, String gtinCode, ProductStatus status, ProductCategories category, Pageable pageable);

}
