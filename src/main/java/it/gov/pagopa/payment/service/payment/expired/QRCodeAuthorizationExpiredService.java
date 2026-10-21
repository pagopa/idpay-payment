package it.gov.pagopa.payment.service.payment.expired;

import it.gov.pagopa.payment.entity.Transaction;

/**
 * This component schedules the expiration of authorized payments
 * */
public interface QRCodeAuthorizationExpiredService {
    Transaction findByTrxCodeAndAuthorizationNotExpired(String toLowerCase);
    Transaction findByTrxCodeAndAuthorizationNotExpiredThrottled(String toLowerCase);
    Long execute();

    Long forceExpiration(String initiativeId);
}
