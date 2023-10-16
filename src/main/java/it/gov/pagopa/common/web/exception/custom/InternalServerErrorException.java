package it.gov.pagopa.common.web.exception.custom;

public class InternalServerErrorException extends ServiceException {

  public InternalServerErrorException(String code, String message) {
    super(code, message);
  }

  public InternalServerErrorException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message, printStackTrace, ex);
  }
}

