package it.gov.pagopa.payment.service.payment.idpaycode.expired;

import it.gov.pagopa.payment.entity.Transaction;

public interface IdpayCodeAuthorizationExpiredService {
    Transaction findByTrxIdAndAuthorizationNotExpired(String toLowerCase);
}
