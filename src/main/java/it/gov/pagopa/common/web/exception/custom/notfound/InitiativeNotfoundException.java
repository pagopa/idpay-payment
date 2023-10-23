package it.gov.pagopa.common.web.exception.custom.notfound;

import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class InitiativeNotfoundException extends ServiceException {

  public InitiativeNotfoundException(String code, String message) {
    super(code, message);
  }

  public InitiativeNotfoundException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
