package it.gov.pagopa.payment.service.payment.barcode.validation;

import it.gov.pagopa.payment.model.TransactionInProgress;
import java.util.Map;

public record BarCodeAdditionalPropertiesValidationInput(
        TransactionInProgress transaction,
        Map<String, String> additionalProperties,
        BarCodeAdditionalPropertiesOperation operation) {
}
