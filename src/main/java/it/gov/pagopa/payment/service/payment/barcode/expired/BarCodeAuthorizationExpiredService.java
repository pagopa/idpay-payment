package it.gov.pagopa.payment.service.payment.barcode.expired;

import it.gov.pagopa.payment.model.TransactionInProgress;

public interface BarCodeAuthorizationExpiredService {
    TransactionInProgress findByTrxCodeAndAuthorizationNotExpired(String toLowerCase);
}
