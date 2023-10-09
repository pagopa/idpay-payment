package it.gov.pagopa.payment.service.payment.idpayCode.expired;

import it.gov.pagopa.payment.model.TransactionInProgress;

public interface IdpayCodeAuthorizationExpiredService {
    TransactionInProgress findByTrxIdAndAuthorizationNotExpired(String toLowerCase);
}
