package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

public class UserNotAllowedException extends ServiceException {

  public UserNotAllowedException(String code, String message) {
    this(code, message, null, false, null);
  }

  public UserNotAllowedException(String code, String message, ServiceExceptionPayload response, boolean printStackTrace, Throwable ex) {
    super(code, message, response, printStackTrace, ex);
  }
}
