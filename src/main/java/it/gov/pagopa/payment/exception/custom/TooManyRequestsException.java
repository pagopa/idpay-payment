package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;

public class TooManyRequestsException extends ServiceException {

  public TooManyRequestsException(String message) {
    this(message, false, null);
  }

  public TooManyRequestsException(String message, boolean printStackTrace, Throwable ex) {
    this(ExceptionCode.TOO_MANY_REQUESTS, message, printStackTrace, ex);
  }
  public TooManyRequestsException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
