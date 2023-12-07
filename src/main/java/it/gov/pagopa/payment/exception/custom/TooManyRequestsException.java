package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.ServiceException;

public class TooManyRequestsException extends ServiceException {

  public TooManyRequestsException(String message) {
    this(message, false, null);
  }

  public TooManyRequestsException(String message, boolean printStackTrace, Throwable ex) {
    super(ExceptionCode.TOO_MANY_REQUESTS, message, printStackTrace, ex);
  }
}
