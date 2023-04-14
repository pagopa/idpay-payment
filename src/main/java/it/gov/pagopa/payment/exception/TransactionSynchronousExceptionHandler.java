package it.gov.pagopa.payment.exception;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class TransactionSynchronousExceptionHandler {

    @ExceptionHandler(TransactionSynchronousException.class)
    protected ResponseEntity<AuthPaymentDTO> handleException( TransactionSynchronousException transactionSynchronousException) {
        return ResponseEntity.status(transactionSynchronousException.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(transactionSynchronousException.getResponse());
    }
}
