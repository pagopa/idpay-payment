package it.gov.pagopa.payment.service.payment.barcode.validation;

import it.gov.pagopa.payment.configuration.BarCodeAdditionalPropertiesValidationProperties;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.exception.custom.InternalServerErrorException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BarCodeAdditionalPropertiesValidationResolver {

    private final Map<BarCodeAdditionalPropertiesValidationType, BarCodeAdditionalPropertiesValidationStrategy> strategiesByType;
    private final BarCodeAdditionalPropertiesValidationProperties properties;

    public BarCodeAdditionalPropertiesValidationResolver(
            List<BarCodeAdditionalPropertiesValidationStrategy> strategies,
            BarCodeAdditionalPropertiesValidationProperties properties) {
        EnumMap<BarCodeAdditionalPropertiesValidationType, BarCodeAdditionalPropertiesValidationStrategy> mappedStrategies =
                new EnumMap<>(BarCodeAdditionalPropertiesValidationType.class);
        for (BarCodeAdditionalPropertiesValidationStrategy strategy : strategies) {
            mappedStrategies.put(strategy.getValidationType(), strategy);
        }
        this.strategiesByType = Collections.unmodifiableMap(mappedStrategies);
        this.properties = properties;
    }

    public BarCodeAdditionalPropertiesValidationStrategy resolve(String initiativeId) {
        BarCodeAdditionalPropertiesValidationType validationType = properties.resolveValidationType(initiativeId);
        BarCodeAdditionalPropertiesValidationStrategy strategy = strategiesByType.get(validationType);
        if (strategy == null) {
            throw new InternalServerErrorException(
                    PaymentConstants.ExceptionCode.GENERIC_ERROR,
                    "Cannot resolve barcode additional properties validation strategy for initiative [%s] and type [%s]"
                            .formatted(initiativeId, validationType));
        }
        return strategy;
    }
}
