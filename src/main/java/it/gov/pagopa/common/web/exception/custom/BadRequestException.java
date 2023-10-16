package it.gov.pagopa.common.web.exception.custom;

public class BadRequestException extends ServiceException {

  public BadRequestException(String code, String message) {
    super(code, message);
  }

  public BadRequestException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}

