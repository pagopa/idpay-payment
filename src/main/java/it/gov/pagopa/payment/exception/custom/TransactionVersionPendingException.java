package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

import static it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode.PAYMENT_TRANSACTION_VERSION_PENDING;

public class TransactionVersionPendingException extends ServiceException {
    public TransactionVersionPendingException(String message, boolean printStackTrace, Throwable ex) {
        this(PAYMENT_TRANSACTION_VERSION_PENDING, message, printStackTrace, ex);
    }
    public TransactionVersionPendingException(String code, String message, boolean printStackTrace, Throwable ex) {
        super(code,message,printStackTrace,ex);
    }
}
