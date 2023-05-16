package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;

public interface QRCodeConfirmationService {
    TransactionResponse confirmPayment(String trxId, String merchantId, String acquirerId);
}
