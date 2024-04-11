package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;

public class PaymentInstrumentInvocationException extends ServiceException {

  public PaymentInstrumentInvocationException(String message, boolean printStackTrace, Throwable ex) {
    this(ExceptionCode.GENERIC_ERROR, message, printStackTrace, ex);
  }

  public PaymentInstrumentInvocationException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message,printStackTrace, ex);
  }
}
