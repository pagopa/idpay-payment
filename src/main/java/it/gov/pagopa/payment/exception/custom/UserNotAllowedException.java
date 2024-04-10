package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

public class UserNotAllowedException extends ServiceException {

  public UserNotAllowedException(String code, String message) {
    this(code, message, false, null);
  }

  public UserNotAllowedException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
