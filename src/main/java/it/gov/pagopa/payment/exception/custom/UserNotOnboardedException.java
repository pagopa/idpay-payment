package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.ServiceException;

public class UserNotOnboardedException extends ServiceException {

  public UserNotOnboardedException(String message,boolean printStackTrace, Throwable ex) {
    this(ExceptionCode.USER_NOT_ONBOARDED, message, null,printStackTrace,ex);
  }

  public UserNotOnboardedException(String code, String message) {
    this(code, message, null, false, null);
  }

  public UserNotOnboardedException(String code, String message, ServiceExceptionPayload response, boolean printStackTrace, Throwable ex) {
    super(code, message, response, printStackTrace, ex);
  }
}
