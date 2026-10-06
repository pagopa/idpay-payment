package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;

public class RewardBatchEligibilityNotAllowedException extends ServiceException {

    public RewardBatchEligibilityNotAllowedException(String message) {
        super(ExceptionCode.REWARD_BATCH_ELIGIBILITY_NOT_ALLOWED, message, false, null);
    }
}
