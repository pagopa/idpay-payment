package it.gov.pagopa.payment.configuration;

import it.gov.pagopa.payment.service.payment.barcode.validation.BarCodeAdditionalPropertiesValidationType;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.bar-code.additional-properties-validation")
@Data
public class BarCodeAdditionalPropertiesValidationProperties {

    private BarCodeAdditionalPropertiesValidationType defaultType = BarCodeAdditionalPropertiesValidationType.NONE;
    private Map<String, BarCodeAdditionalPropertiesValidationType> initiatives = new HashMap<>();

    public BarCodeAdditionalPropertiesValidationType resolveValidationType(String initiativeId) {
        return initiatives.getOrDefault(initiativeId, defaultType);
    }
}
