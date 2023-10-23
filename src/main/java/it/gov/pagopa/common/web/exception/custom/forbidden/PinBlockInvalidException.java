package it.gov.pagopa.common.web.exception.custom.forbidden;

import it.gov.pagopa.common.web.exception.custom.ServiceException;

public class PinBlockInvalidException extends ServiceException {

  public PinBlockInvalidException(String code, String message) {
    super(code, message);
  }

  public PinBlockInvalidException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
