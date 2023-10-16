package it.gov.pagopa.common.web.exception.custom;

public class ForbiddenException extends ServiceException {
  public ForbiddenException(String code, String message) {
    super(code, message);
  }

  public ForbiddenException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
