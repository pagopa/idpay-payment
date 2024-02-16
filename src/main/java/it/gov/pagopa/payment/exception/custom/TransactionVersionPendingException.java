package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode.PAYMENT_TRANSACTION_VERSION_PENDING;

public class TransactionVersionPendingException extends ServiceException {
    public TransactionVersionPendingException(String message, boolean printStackTrace, Throwable ex) {
        this(PAYMENT_TRANSACTION_VERSION_PENDING, message, null, printStackTrace, ex);
    }
    public TransactionVersionPendingException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code,message,payload,printStackTrace,ex);
    }
}
