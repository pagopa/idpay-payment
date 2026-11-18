package it.gov.pagopa.payment.exception;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransactionSynchronousExceptionHandler {
    @ExceptionHandler(TransactionSynchronousException.class)
    protected ResponseEntity<AuthPaymentDTO> handleException( TransactionSynchronousException transactionSynchronousException) {
        return ResponseEntity.status(transactionSynchronousException.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(transactionSynchronousException.getResponse());
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDTO> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        return new ResponseEntity<>(new ErrorDTO("INVALID_REQUEST", ex.getMessage()),
                HttpStatus.BAD_REQUEST);
    }
}
