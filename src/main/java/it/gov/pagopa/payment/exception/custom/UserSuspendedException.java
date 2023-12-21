package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.ServiceException;

public class UserSuspendedException extends ServiceException {

  public UserSuspendedException(String message) {
    this(ExceptionCode.USER_SUSPENDED_ERROR, message);
  }

  public UserSuspendedException(String code, String message) {
    this(code, message, null, false, null);
  }

  public UserSuspendedException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
    super(code, message, payload, printStackTrace, ex);
  }
}
