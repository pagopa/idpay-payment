package it.gov.pagopa.payment.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceError {
    private String code;
    private String message;

    public ServiceError(String code, String message) {
      this.code = code;
      this.message = message;
    }
}
