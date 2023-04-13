package it.gov.pagopa.payment.exception;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class TransactionSynchronousExceptionHandler {

  @ExceptionHandler(TransactionSynchronousException.class)
  protected ResponseEntity<AuthPaymentDTO> handleException(
      TransactionSynchronousException transactionSynchronousException) {
    AuthPaymentDTO response;
    HttpStatus httpStatus;
    httpStatus = HttpStatus.BAD_REQUEST;
    response = transactionSynchronousException.getResponse();

    return ResponseEntity.status(httpStatus)
        .contentType(MediaType.APPLICATION_JSON)
        .body(response);
  }
}
