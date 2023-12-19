package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceExceptionResponse;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.ServiceException;

public class UserNotOnboardedException extends ServiceException {

  public UserNotOnboardedException(String message) {
    this(ExceptionCode.USER_NOT_ONBOARDED, message);
  }

  public UserNotOnboardedException(String code, String message) {
    this(code, message, null, false, null);
  }

  public UserNotOnboardedException(String code, String message, ServiceExceptionResponse response, boolean printStackTrace, Throwable ex) {
    super(code, message, response, printStackTrace, ex);
  }
}
