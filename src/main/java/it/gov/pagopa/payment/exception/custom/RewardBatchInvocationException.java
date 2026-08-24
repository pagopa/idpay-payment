package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;

public class RewardBatchInvocationException extends ServiceException {

    public RewardBatchInvocationException(String message, boolean printStackTrace, Throwable ex) {
        super(ExceptionCode.GENERIC_ERROR, message, printStackTrace, ex);
    }
}
