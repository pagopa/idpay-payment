package it.gov.pagopa.payment.service.payment.idpaycode.expired;

import it.gov.pagopa.payment.model.TransactionInProgress;

public interface IdpayCodeAuthorizationExpiredService {
    TransactionInProgress findByTrxIdAndAuthorizationNotExpired(String toLowerCase);
}
