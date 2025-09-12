package it.gov.pagopa.payment.service.payment;


import it.gov.pagopa.payment.connector.rest.register.RegisterConnector;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductDTO;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductListDTO;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductStatus;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.exception.custom.ProductNotValidException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentCheckServiceImpl implements PaymentCheckService {

    private final RegisterConnector registerConnector;

    public PaymentCheckServiceImpl(RegisterConnector registerConnector) {
        this.registerConnector = registerConnector;
    }

    @Override
    public ProductDTO validateProduct(String productGtin) {
        ProductListDTO productListDTO = registerConnector.getProductList(productGtin, ProductStatus.APPROVED);
        if(productListDTO == null || productListDTO.getContent().isEmpty()){
            throw new ProductNotValidException(PaymentConstants.ExceptionCode.PRODUCT_NOT_FOUND,
                    String.format("The product with gtin [%s] is not found or not approved", productGtin));
        }
        return productListDTO.getContent().getFirst();
    }

}
