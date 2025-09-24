package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeEnrichedResponse;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;

public interface BarCodeCreationService {
    TransactionBarCodeResponse createTransaction(TransactionBarCodeCreationRequest trxBRCodeCreationRequest, String channel, String userId);
    TransactionBarCodeEnrichedResponse createExtendedTransaction(TransactionBarCodeCreationRequest trxBarCodeCreationRequest, String channel, String userId);
}
