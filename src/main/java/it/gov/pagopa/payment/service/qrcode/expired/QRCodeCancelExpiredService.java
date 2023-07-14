package it.gov.pagopa.payment.service.qrcode.expired;

/**
 * This component schedules the expiration of cancelled payments
 * */
public interface QRCodeCancelExpiredService {
    Long execute();
    Long forceExpiration(String initiativeId);
}
