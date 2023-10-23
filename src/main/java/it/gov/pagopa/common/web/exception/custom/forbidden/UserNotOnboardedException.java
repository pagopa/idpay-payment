package it.gov.pagopa.common.web.exception.custom.forbidden;

import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class UserNotOnboardedException extends ServiceException {

  public UserNotOnboardedException(String code, String message) {
    super(code, message);
  }

  public UserNotOnboardedException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
