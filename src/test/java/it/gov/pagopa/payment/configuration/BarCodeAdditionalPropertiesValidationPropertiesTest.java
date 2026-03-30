package it.gov.pagopa.payment.configuration;

import it.gov.pagopa.payment.service.payment.barcode.validation.BarCodeAdditionalPropertiesValidationType;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@TestPropertySource(properties = {
        "app.barCode.additional-properties-validation.default-type=PRODUCT_GTIN",
        "app.barCode.additional-properties-validation.initiatives.INITIATIVE_A=NONE"
})
@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(BarCodeAdditionalPropertiesValidationProperties.class)
class BarCodeAdditionalPropertiesValidationPropertiesTest {

    @Autowired
    private BarCodeAdditionalPropertiesValidationProperties properties;

    @Test
    void shouldBindLegacyBarCodePropertyNames() {
        Assertions.assertEquals(BarCodeAdditionalPropertiesValidationType.PRODUCT_GTIN, properties.getDefaultType());
        Assertions.assertEquals(Map.of("INITIATIVE_A", BarCodeAdditionalPropertiesValidationType.NONE), properties.getInitiatives());
    }
}
