package it.gov.pagopa.payment.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

public class InternalServerErrorException extends ServiceException {

  public InternalServerErrorException(String code, String message) {
    this(code, message, null, false, null);
  }

  public InternalServerErrorException(String code, String message, ServiceExceptionPayload response, boolean printStackTrace, Throwable ex) {
    super(code, message, response, printStackTrace, ex);
  }
}
