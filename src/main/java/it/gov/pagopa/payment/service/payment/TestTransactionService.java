package it.gov.pagopa.payment.service.payment;

import java.time.OffsetDateTime;

public interface TestTransactionService {

    void updateTrxEndDate(String id, String initiativeId, String userId, OffsetDateTime newEndDate);
}
