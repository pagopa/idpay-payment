package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.model.TransactionInProgress;

public interface QRCodeConfirmationService {
    TransactionResponse confirmPayment(String trxId, String merchantId, String acquirerId);
    void confirmAuthorizedPayment(TransactionInProgress trx);
}
