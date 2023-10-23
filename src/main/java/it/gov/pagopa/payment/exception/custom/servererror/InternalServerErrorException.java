package it.gov.pagopa.payment.exception.custom.servererror;

import it.gov.pagopa.payment.exception.custom.ServiceException;

public class InternalServerErrorException extends ServiceException {

  public InternalServerErrorException(String code, String message) {
    this(code, message, false, null);
  }

  public InternalServerErrorException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
