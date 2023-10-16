package it.gov.pagopa.common.web.exception.custom;

public class TooManyRequestsException extends ServiceException {

  public TooManyRequestsException(String code, String message) {
    super(code, message);
  }

  public TooManyRequestsException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}

