package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionResponse;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;

public class PDVInvocationException extends ServiceException {

  public PDVInvocationException(String message) {
    this(message, false, null);
  }

  public PDVInvocationException(String message, boolean printStackTrace, Throwable ex) {
    this(ExceptionCode.GENERIC_ERROR, message, null,printStackTrace, ex);
  }
  public PDVInvocationException(String code, String message, ServiceExceptionResponse response, boolean printStackTrace, Throwable ex) {
    super(code, message, response, printStackTrace, ex);
  }
}
