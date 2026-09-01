package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

public class TransactionConflictException extends ServiceException {

    public TransactionConflictException(String code, String message) {
        super(code, message, false, null);
    }
}
