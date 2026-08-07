package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.payment.entity.Transaction;

public interface RewardBatchEligibilityPreflightService {
    void verifyEligibility(Transaction transaction, String merchantId, String authorization);
}
