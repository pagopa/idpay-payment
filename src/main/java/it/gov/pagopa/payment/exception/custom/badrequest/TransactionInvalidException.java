package it.gov.pagopa.payment.exception.custom.badrequest;

import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class TransactionInvalidException extends ServiceException {

  public TransactionInvalidException(String code, String message) {
    this(code, message, false, null);
  }

  public TransactionInvalidException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
