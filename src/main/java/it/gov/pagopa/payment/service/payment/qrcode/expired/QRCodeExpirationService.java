package it.gov.pagopa.payment.service.payment.qrcode.expired;

/**
 * This component schedules payment deadlines
 * */
public interface QRCodeExpirationService {
    Long forceConfirmTrxExpiration(String initiativeId);
    Long forceAuthorizationTrxExpiration(String initiativeId);
}
