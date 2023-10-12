package it.gov.pagopa.payment.service.payment.expired;

import it.gov.pagopa.payment.model.TransactionInProgress;

/**
 * This component schedules the expiration of authorized payments
 * */
public interface QRCodeAuthorizationExpiredService {
    TransactionInProgress findByTrxCodeAndAuthorizationNotExpired(String toLowerCase);
    TransactionInProgress findByTrxCodeAndAuthorizationNotExpiredThrottled(String toLowerCase);
    Long execute();

    Long forceExpiration(String initiativeId);
}
