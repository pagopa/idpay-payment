package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.dto.brcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.brcode.TransactionBarCodeResponse;

public interface BarCodeCreationService {
    TransactionBarCodeResponse createTransaction(TransactionBarCodeCreationRequest trxBRCodeCreationRequest, String channel, String userId);
}
