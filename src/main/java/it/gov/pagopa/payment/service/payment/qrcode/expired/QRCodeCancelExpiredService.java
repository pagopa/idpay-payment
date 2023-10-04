package it.gov.pagopa.payment.service.payment.qrcode.expired;

/**
 * This component schedules the expiration of cancelled payments
 * */
public interface QRCodeCancelExpiredService {
    Long execute();
    Long forceExpiration(String initiativeId);
}
