package it.gov.pagopa.payment.service.payment.barcode.validation;

import it.gov.pagopa.payment.configuration.BarCodeAdditionalPropertiesValidationProperties;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.exception.custom.InternalServerErrorException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarCodeAdditionalPropertiesValidationResolverTest {

    @Mock
    private BarCodeAdditionalPropertiesValidationStrategy noOpStrategy;
    @Mock
    private BarCodeAdditionalPropertiesValidationStrategy productGtinStrategy;

    private BarCodeAdditionalPropertiesValidationProperties properties;

    @BeforeEach
    void setUp() {
        properties = new BarCodeAdditionalPropertiesValidationProperties();
    }

    @Test
    void resolve_shouldReturnInitiativeSpecificStrategy() {
        when(noOpStrategy.getValidationType()).thenReturn(BarCodeAdditionalPropertiesValidationType.NONE);
        when(productGtinStrategy.getValidationType()).thenReturn(BarCodeAdditionalPropertiesValidationType.PRODUCT_GTIN);
        properties.setInitiatives(Map.of("INITIATIVE_ID", BarCodeAdditionalPropertiesValidationType.PRODUCT_GTIN));

        BarCodeAdditionalPropertiesValidationResolver resolver = new BarCodeAdditionalPropertiesValidationResolver(
                List.of(noOpStrategy, productGtinStrategy),
                properties);

        Assertions.assertSame(productGtinStrategy, resolver.resolve("INITIATIVE_ID"));
    }

    @Test
    void resolve_shouldReturnDefaultNoneStrategyWhenInitiativeIsNotConfigured() {
        when(noOpStrategy.getValidationType()).thenReturn(BarCodeAdditionalPropertiesValidationType.NONE);
        when(productGtinStrategy.getValidationType()).thenReturn(BarCodeAdditionalPropertiesValidationType.PRODUCT_GTIN);

        BarCodeAdditionalPropertiesValidationResolver resolver = new BarCodeAdditionalPropertiesValidationResolver(
                List.of(noOpStrategy, productGtinStrategy),
                properties);

        Assertions.assertSame(noOpStrategy, resolver.resolve("INITIATIVE_ID"));
    }

    @Test
    void resolve_shouldThrowWhenConfiguredStrategyIsMissing() {
        when(noOpStrategy.getValidationType()).thenReturn(BarCodeAdditionalPropertiesValidationType.NONE);
        properties.setDefaultType(BarCodeAdditionalPropertiesValidationType.PRODUCT_GTIN);

        BarCodeAdditionalPropertiesValidationResolver resolver = new BarCodeAdditionalPropertiesValidationResolver(
                List.of(noOpStrategy),
                properties);

        InternalServerErrorException result = Assertions.assertThrows(InternalServerErrorException.class,
                () -> resolver.resolve("INITIATIVE_ID"));

        Assertions.assertEquals(PaymentConstants.ExceptionCode.GENERIC_ERROR, result.getCode());
        Assertions.assertTrue(result.getMessage().contains("INITIATIVE_ID"));
        Assertions.assertTrue(result.getMessage().contains(BarCodeAdditionalPropertiesValidationType.PRODUCT_GTIN.name()));
    }
}
