package it.gov.pagopa.payment.service.payment.barcode.validation;

import java.util.Map;

public interface BarCodeAdditionalPropertiesValidationStrategy {

    BarCodeAdditionalPropertiesValidationType getValidationType();

    Map<String, String> validateAndEnrich(BarCodeAdditionalPropertiesValidationInput input);
}
