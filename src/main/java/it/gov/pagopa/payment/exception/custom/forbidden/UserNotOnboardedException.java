package it.gov.pagopa.payment.exception.custom.forbidden;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.exception.custom.ServiceException;

public class UserNotOnboardedException extends ServiceException {

  public UserNotOnboardedException(String message) {
    this(ExceptionCode.USER_NOT_ONBOARDED, message);
  }

  public UserNotOnboardedException(String code, String message) {
    this(code, message, false, null);
  }

  public UserNotOnboardedException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
