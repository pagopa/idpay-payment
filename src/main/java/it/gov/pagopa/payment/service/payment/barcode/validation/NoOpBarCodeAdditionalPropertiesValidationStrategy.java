package it.gov.pagopa.payment.service.payment.barcode.validation;

import java.util.Collections;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class NoOpBarCodeAdditionalPropertiesValidationStrategy implements BarCodeAdditionalPropertiesValidationStrategy {

    @Override
    public BarCodeAdditionalPropertiesValidationType getValidationType() {
        return BarCodeAdditionalPropertiesValidationType.NONE;
    }

    @Override
    public Map<String, String> validateAndEnrich(Map<String, String> additionalProperties, BarCodeAdditionalPropertiesOperation operation) {
        return Collections.emptyMap();
    }
}
