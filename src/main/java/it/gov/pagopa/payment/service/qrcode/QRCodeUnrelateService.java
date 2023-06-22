package it.gov.pagopa.payment.service.qrcode;

public interface QRCodeUnrelateService {
    void unrelateTransaction(String trxCode, String userId);
}
