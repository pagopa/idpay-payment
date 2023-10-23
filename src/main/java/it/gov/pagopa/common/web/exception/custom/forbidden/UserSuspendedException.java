package it.gov.pagopa.common.web.exception.custom.forbidden;

import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class UserSuspendedException extends ServiceException {

  public UserSuspendedException(String code, String message) {
    super(code, message);
  }

  public UserSuspendedException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
