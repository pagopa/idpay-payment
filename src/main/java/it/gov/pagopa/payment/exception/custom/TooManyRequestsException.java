package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceExceptionResponse;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.ServiceException;

public class TooManyRequestsException extends ServiceException {

  public TooManyRequestsException(String message) {
    this(message, false, null);
  }

  public TooManyRequestsException(String message, boolean printStackTrace, Throwable ex) {
    this(ExceptionCode.TOO_MANY_REQUESTS, message, null, printStackTrace, ex);
  }
  public TooManyRequestsException(String code, String message, ServiceExceptionResponse response, boolean printStackTrace, Throwable ex) {
    super(code, message, response, printStackTrace, ex);
  }
}
