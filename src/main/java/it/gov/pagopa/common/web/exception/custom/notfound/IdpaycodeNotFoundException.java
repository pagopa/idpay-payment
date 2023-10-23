package it.gov.pagopa.common.web.exception.custom.notfound;

import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class IdpaycodeNotFoundException extends ServiceException {

  public IdpaycodeNotFoundException(String code, String message) {
    super(code, message);
  }

  public IdpaycodeNotFoundException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
