package it.gov.pagopa.common.web.exception.custom.forbidden;

import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class InitiativeInvalidException extends ServiceException {

  public InitiativeInvalidException(String code, String message) {
    super(code, message);
  }

  public InitiativeInvalidException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
