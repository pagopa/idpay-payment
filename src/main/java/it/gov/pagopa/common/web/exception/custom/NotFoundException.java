package it.gov.pagopa.common.web.exception.custom;

public class NotFoundException extends ServiceException {

  public NotFoundException(String code, String message) {
    super(code, message);
  }

  public NotFoundException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}
