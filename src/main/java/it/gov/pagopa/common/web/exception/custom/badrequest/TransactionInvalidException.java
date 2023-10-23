package it.gov.pagopa.common.web.exception.custom.badrequest;

import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class TransactionInvalidException extends ServiceException {

  public TransactionInvalidException(String code, String message) {
    super(code, message);
  }

  public TransactionInvalidException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
