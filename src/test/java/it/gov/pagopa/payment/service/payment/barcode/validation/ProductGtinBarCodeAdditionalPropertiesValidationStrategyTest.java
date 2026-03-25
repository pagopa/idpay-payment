package it.gov.pagopa.payment.service.payment.barcode.validation;

import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductDTO;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.service.payment.PaymentCheckService;
import it.gov.pagopa.payment.test.fakers.ProductDTOFaker;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductGtinBarCodeAdditionalPropertiesValidationStrategyTest {

    @Mock
    private PaymentCheckService paymentCheckService;

    private ProductGtinBarCodeAdditionalPropertiesValidationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ProductGtinBarCodeAdditionalPropertiesValidationStrategy(paymentCheckService);
    }

    @Test
    void getValidationType() {
        Assertions.assertEquals(BarCodeAdditionalPropertiesValidationType.PRODUCT_GTIN, strategy.getValidationType());
    }

    @Test
    void validateAndEnrich_shouldThrowForMissingProductGtinDuringPreview() {
        TransactionInvalidException result = Assertions.assertThrows(TransactionInvalidException.class,
                () -> strategy.validateAndEnrich(new BarCodeAdditionalPropertiesValidationInput(
                        null,
                        null,
                        BarCodeAdditionalPropertiesOperation.PREVIEW)));

        Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_ADDITIONAL_PROPERTIES_NOT_EXIST, result.getCode());
        Assertions.assertTrue(result.getMessage().contains("preview"));
    }

    @Test
    void validateAndEnrich_shouldReturnValidatedProductInformation() {
        ProductDTO productDTO = ProductDTOFaker.mockInstance();
        when(paymentCheckService.validateProduct("123123")).thenReturn(productDTO);

        Map<String, String> result = strategy.validateAndEnrich(new BarCodeAdditionalPropertiesValidationInput(
                null,
                Map.of("productGtin", "123123"),
                BarCodeAdditionalPropertiesOperation.AUTHORIZE));

        Assertions.assertEquals(productDTO.getProductName(), result.get("productName"));
        Assertions.assertEquals(productDTO.getGtinCode(), result.get("productGtin"));
        verify(paymentCheckService).validateProduct("123123");
    }
}
