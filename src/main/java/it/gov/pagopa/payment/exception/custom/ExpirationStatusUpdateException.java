package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.constants.PaymentConstants;

public class ExpirationStatusUpdateException extends ServiceException {

    public ExpirationStatusUpdateException(String message) {
        this(PaymentConstants.ExceptionCode.GENERIC_ERROR, message);
    }

    public ExpirationStatusUpdateException(String code, String message) {
        this(code, message, false, null);
    }

    public ExpirationStatusUpdateException(String code, String message, boolean printStackTrace, Throwable ex) {
        super(code, message, printStackTrace, ex);
    }
}
