package it.gov.pagopa.payment.service.payment.qrcode;

public interface QRCodeCancelService {
    void cancelTransaction(String trxId, String merchantId, String acquirerId);
}
