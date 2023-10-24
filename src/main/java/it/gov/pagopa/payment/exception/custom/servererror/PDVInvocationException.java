package it.gov.pagopa.payment.exception.custom.servererror;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.ServiceException;

public class PDVInvocationException extends ServiceException {

  public PDVInvocationException(String message) {
    this(message, false, null);
  }

  public PDVInvocationException(String message, boolean printStackTrace, Throwable ex) {
    super(ExceptionCode.GENERIC_ERROR, message, printStackTrace, ex);
  }
}
