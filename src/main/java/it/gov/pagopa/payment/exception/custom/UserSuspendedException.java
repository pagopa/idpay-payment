package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;

public class UserSuspendedException extends ServiceException {

  public UserSuspendedException(String message) {
    this(ExceptionCode.USER_SUSPENDED_ERROR, message);
  }

  public UserSuspendedException(String code, String message) {
    this(code, message, false, null);
  }

  public UserSuspendedException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
