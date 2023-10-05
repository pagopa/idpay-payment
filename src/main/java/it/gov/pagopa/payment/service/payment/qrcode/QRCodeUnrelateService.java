package it.gov.pagopa.payment.service.payment.qrcode;

public interface QRCodeUnrelateService {
    void unrelateTransaction(String trxCode, String userId);
}
