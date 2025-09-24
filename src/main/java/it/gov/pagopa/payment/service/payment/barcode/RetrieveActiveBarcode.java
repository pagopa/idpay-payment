package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;

public interface RetrieveActiveBarcode {
    TransactionBarCodeResponse findOldestNotAuthorized(String userId, String initiativeId);
}
