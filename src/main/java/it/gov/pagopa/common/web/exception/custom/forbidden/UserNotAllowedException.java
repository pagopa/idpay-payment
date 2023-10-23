package it.gov.pagopa.common.web.exception.custom.forbidden;

import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class UserNotAllowedException extends ServiceException {

  public UserNotAllowedException(String code, String message) {
    super(code, message);
  }

  public UserNotAllowedException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
