package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode.PAYMENT_TRANSACTION_VERSION_MISMATCH;

public class TransactionVersionMismatchException extends ServiceException {
    public TransactionVersionMismatchException(String message, boolean printStackTrace, Throwable ex) {
        this(PAYMENT_TRANSACTION_VERSION_MISMATCH, message, null, printStackTrace, ex);
    }
    public TransactionVersionMismatchException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code,message,payload,printStackTrace,ex);
    }
}
