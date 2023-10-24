package it.gov.pagopa.payment.exception.custom.servererror;

import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class PaymentInstrumentInvocationException extends ServiceException {

   public PaymentInstrumentInvocationException(String message) {
     this(message, false, null);
  }

  public PaymentInstrumentInvocationException(String message, boolean printStackTrace, Throwable ex) {
    super(ExceptionCode.GENERIC_ERROR, message, printStackTrace, ex);
  }
}
