package it.gov.pagopa.payment.exception;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class TransactionSynchronousException extends RuntimeException{
  private final HttpStatus httpStatus;
  private final AuthPaymentDTO response;

  public TransactionSynchronousException(HttpStatus httpStatus, AuthPaymentDTO response) {
    this.httpStatus = httpStatus;
    this.response = response;
  }
}
