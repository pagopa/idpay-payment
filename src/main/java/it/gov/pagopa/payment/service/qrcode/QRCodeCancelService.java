package it.gov.pagopa.payment.service.qrcode;

public interface QRCodeCancelService {
    void cancelTransaction(String trxId, String merchantId, String acquirerId);
    void cancelTransaction(String trxCode, String userId);
}
