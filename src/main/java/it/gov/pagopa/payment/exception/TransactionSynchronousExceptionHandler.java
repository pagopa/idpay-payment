package it.gov.pagopa.payment.exception;

import it.gov.pagopa.payment.dto.qrcode.AuthPaymentDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class TransactionSynchronousExceptionHandler {

  @ExceptionHandler(RuntimeException.class)
  protected ResponseEntity<AuthPaymentDTO> handleException(RuntimeException error){
    AuthPaymentDTO response;
    HttpStatus httpStatus;
    if (error instanceof TransactionSynchronousException transactionSynchronousException) {
      httpStatus = HttpStatus.BAD_REQUEST;
      response = transactionSynchronousException.getResponse();

    } else {
      httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
      response = null;
    }
    return ResponseEntity.status(httpStatus)
        .contentType(MediaType.APPLICATION_JSON)
        .body(response);
  }
}
