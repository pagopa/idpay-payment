package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionResponse;

public class InternalServerErrorException extends ServiceException {

  public InternalServerErrorException(String code, String message) {
    this(code, message, null, false, null);
  }

  public InternalServerErrorException(String code, String message, ServiceExceptionResponse response, boolean printStackTrace, Throwable ex) {
    super(code, message, response, printStackTrace, ex);
  }
}
