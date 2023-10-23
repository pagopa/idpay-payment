package it.gov.pagopa.common.web.exception.custom.servererror;

import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class PaymentInstrumentInvocationException extends ServiceException {

  public PaymentInstrumentInvocationException(String code, String message) {
    super(code, message);
  }

  public PaymentInstrumentInvocationException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
