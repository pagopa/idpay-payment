package it.gov.pagopa.payment.exception;

import it.gov.pagopa.payment.model.error.Problem;
import it.gov.pagopa.payment.model.error.ProblemError;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Slf4j
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      @NonNull MethodArgumentNotValidException ex, HttpHeaders headers, @NonNull HttpStatus status,
      @NonNull WebRequest request) {
    log.error("InvalidRequestException Occured --> MESSAGE:{}, STATUS: {}", ex.getMessage(),
        HttpStatus.BAD_REQUEST, ex);
    headers.setContentType(MediaType.APPLICATION_JSON);
    Problem problem = createProblem("INVALID ARGUMENT", HttpStatus.BAD_REQUEST.value(), "0000");
    return new ResponseEntity<>(problem, headers, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(InvalidRequestException.class)
  public ResponseEntity<Problem> handleInvalidRequestException(HttpServletRequest request,
      InvalidRequestException ex) {
    log.error("InvalidRequestException Occured --> URL:{}, MESSAGE:{}, STATUS:{}",
        request.getRequestURL(), ex.getMessage(), HttpStatus.BAD_REQUEST, ex);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Problem problem = createProblem(request.getRequestURL().toString(),
        HttpStatus.BAD_REQUEST.value(), ex.getCode());
    return new ResponseEntity<>(problem, headers, HttpStatus.BAD_REQUEST);
  }

  private Problem createProblem(String errorMessage, Integer status, String code) {
    Problem problem = new Problem();
    problem.setStatus(status);
    problem.setErrors(createProblemError(errorMessage, code));
    return problem;
  }

  private List<ProblemError> createProblemError(String message, String code) {
    List<ProblemError> list = new ArrayList<>();
    list.add(ProblemError.builder()
        .code(code)
        .detail(message)
        .build());
    return list;
  }
}
