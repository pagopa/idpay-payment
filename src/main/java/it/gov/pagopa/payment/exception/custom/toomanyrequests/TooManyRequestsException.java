package it.gov.pagopa.payment.exception.custom.toomanyrequests;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.exception.custom.ServiceException;

public class TooManyRequestsException extends ServiceException {

  public TooManyRequestsException(String code, String message) {
    this(message, false, null);
  }

  public TooManyRequestsException(String message, boolean printStackTrace, Throwable ex) {
    super(ExceptionCode.TOO_MANY_REQUESTS, message, printStackTrace, ex);
  }
}
