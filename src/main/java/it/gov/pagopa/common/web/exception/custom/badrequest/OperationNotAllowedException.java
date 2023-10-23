package it.gov.pagopa.common.web.exception.custom.badrequest;

import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class OperationNotAllowedException extends ServiceException {

  public OperationNotAllowedException(String code, String message) {
    super(code, message);
  }

  public OperationNotAllowedException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
