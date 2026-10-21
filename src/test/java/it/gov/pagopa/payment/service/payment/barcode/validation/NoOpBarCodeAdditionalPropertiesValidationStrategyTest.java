package it.gov.pagopa.payment.service.payment.barcode.validation;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NoOpBarCodeAdditionalPropertiesValidationStrategyTest {

    private final NoOpBarCodeAdditionalPropertiesValidationStrategy strategy =
            new NoOpBarCodeAdditionalPropertiesValidationStrategy();

    @Test
    void getValidationType() {
        Assertions.assertEquals(BarCodeAdditionalPropertiesValidationType.NONE, strategy.getValidationType());
    }

    @Test
    void validateAndEnrich_shouldReturnSameAdditionalProperties() {
        Map<String, String> result = strategy.validateAndEnrich(
                Map.of("customField", "customValue"),
                BarCodeAdditionalPropertiesOperation.AUTHORIZE, "INITIATIVE_ID");

        Assertions.assertEquals(Map.of("customField", "customValue"), result);
    }

    @Test
    void validateAndEnrich_shouldReturnEmptyMapWhenInputIsNull() {
        Map<String, String> result = strategy.validateAndEnrich(
                null,
                BarCodeAdditionalPropertiesOperation.PREVIEW, "INITIATIVE_ID");

        Assertions.assertTrue(result.isEmpty());
    }
}
