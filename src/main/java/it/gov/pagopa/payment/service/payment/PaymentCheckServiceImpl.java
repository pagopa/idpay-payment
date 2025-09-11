package it.gov.pagopa.payment.service.payment;


import it.gov.pagopa.payment.connector.rest.register.RegisterConnector;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductCategories;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductListDTO;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductStatus;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.exception.custom.ProductNotFoundException;
import it.gov.pagopa.payment.exception.custom.ProductNotValidException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentCheckServiceImpl implements PaymentCheckService {

    private final RegisterConnector registerConnector;

    public PaymentCheckServiceImpl(RegisterConnector registerConnector) {
        this.registerConnector = registerConnector;
    }

    @Override
    public ProductListDTO validateProduct(String role, String organizationId, String productName, String productFileId, String eprelCode, String gtinCode, ProductStatus status, ProductCategories category, Pageable pageable) {
        ProductListDTO productListDTO = registerConnector.getProductList(role, organizationId, productName, productFileId, eprelCode, gtinCode, status, category, pageable);
        if(productListDTO == null || productListDTO.getContent().isEmpty()){
            throw new ProductNotValidException(PaymentConstants.ExceptionCode.PRODUCT_NOT_FOUND,
                    String.format("The product with gtin [%s] is not found or not approved", gtinCode));
        }
        return productListDTO;
    }

}
