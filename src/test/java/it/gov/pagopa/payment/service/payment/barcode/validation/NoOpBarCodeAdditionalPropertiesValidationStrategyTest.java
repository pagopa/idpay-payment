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
    void validateAndEnrich_shouldReturnEmptyMap() {
        Map<String, String> result = strategy.validateAndEnrich(new BarCodeAdditionalPropertiesValidationInput(
                null,
                Map.of("customField", "customValue"),
                BarCodeAdditionalPropertiesOperation.AUTHORIZE));

        Assertions.assertTrue(result.isEmpty());
    }
}
