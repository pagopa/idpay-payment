package it.gov.pagopa.common.web.exception.custom.forbidden;

import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class TransactionAlreadyAuthorizedException extends ServiceException {

  public TransactionAlreadyAuthorizedException(String code, String message) {
    super(code, message);
  }

  public TransactionAlreadyAuthorizedException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
