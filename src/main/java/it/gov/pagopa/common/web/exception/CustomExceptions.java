package it.gov.pagopa.common.web.exception;

import lombok.Data;
import lombok.Getter;

@Data
public class CustomExceptions {

  private CustomExceptions() {}

  @Getter
  public static class BadRequestException extends RuntimeException {
    private final String code;
    private final String message;

    public BadRequestException(String code, String message) {
      this.code = code;
      this.message = message;
    }
  }
  @Getter
  public static class ForbiddenException extends RuntimeException {
    private final String code;
    private final String message;

    public ForbiddenException(String code, String message) {
      this.code = code;
      this.message = message;
    }
  }
  @Getter
  public static class NotFoundException extends RuntimeException {
    private final String code;
    private final String message;

    public NotFoundException(String code, String message) {
      this.code = code;
      this.message = message;
    }
  }

}
