package it.gov.pagopa.payment.service.payment.barcode.validation;

import it.gov.pagopa.payment.connector.rest.register.dto.ProductDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.service.payment.PaymentCheckService;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import static it.gov.pagopa.payment.utils.Utilities.sanitizeString;

@Service
public class ProductGtinBarCodeAdditionalPropertiesValidationStrategy implements BarCodeAdditionalPropertiesValidationStrategy {

    private static final String PRODUCT_NAME_KEY = "productName";
    private static final String PRODUCT_GTIN_KEY = "productGtin";

    private final PaymentCheckService paymentCheckService;

    public ProductGtinBarCodeAdditionalPropertiesValidationStrategy(PaymentCheckService paymentCheckService) {
        this.paymentCheckService = paymentCheckService;
    }

    @Override
    public BarCodeAdditionalPropertiesValidationType getValidationType() {
        return BarCodeAdditionalPropertiesValidationType.PRODUCT_GTIN;
    }

    @Override
    public Map<String, String> validateAndEnrich(BarCodeAdditionalPropertiesValidationInput input) {
        String productGtin = sanitizeString(input.additionalProperties() != null ? input.additionalProperties().get(PRODUCT_GTIN_KEY) : null);
        if (StringUtils.isBlank(productGtin)) {
            throw invalidAdditionalProperties(input.operation());
        }

        ProductDTO productDTO = paymentCheckService.validateProduct(productGtin);
        Map<String, String> additionalProperties = new HashMap<>();
        additionalProperties.put(PRODUCT_NAME_KEY, productDTO.getProductName());
        additionalProperties.put(PRODUCT_GTIN_KEY, productDTO.getGtinCode());
        return additionalProperties;
    }

    private static TransactionInvalidException invalidAdditionalProperties(BarCodeAdditionalPropertiesOperation operation) {
        String action = operation == BarCodeAdditionalPropertiesOperation.PREVIEW ? "preview" : "authorize";
        return new TransactionInvalidException(
                PaymentConstants.ExceptionCode.TRX_ADDITIONAL_PROPERTIES_NOT_EXIST,
                "Cannot %s transaction with invalid AdditionalProperties".formatted(action));
    }
}
