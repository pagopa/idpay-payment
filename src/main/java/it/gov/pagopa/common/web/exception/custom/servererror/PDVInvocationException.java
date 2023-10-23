package it.gov.pagopa.common.web.exception.custom.servererror;

import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class PDVInvocationException extends ServiceException {

  public PDVInvocationException(String code, String message) {
    super(code, message);
  }

  public PDVInvocationException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
