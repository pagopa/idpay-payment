package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode.PAYMENT_TRANSACTION_VERSION_MISMATCH;

public class TransactionVersionMismatchException extends ServiceException {
    public TransactionVersionMismatchException(String message){
        this(PAYMENT_TRANSACTION_VERSION_MISMATCH,message);
    }
    public TransactionVersionMismatchException(String code, String message) {
        this(code, message,null, false, null);
    }
    public TransactionVersionMismatchException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code,message,payload,printStackTrace,ex);
    }
}
