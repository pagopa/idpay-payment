package it.gov.pagopa.payment.service.payment.barcode.expired;

import it.gov.pagopa.payment.entity.Transaction;

public interface BarCodeAuthorizationExpiredService {
    Transaction findByTrxCodeAndAuthorizationNotExpired(String toLowerCase);
    Transaction findByTrxCodeAndTrxEndDateGreaterThanEqualAndStatusNot(String toLowerCase);

}
