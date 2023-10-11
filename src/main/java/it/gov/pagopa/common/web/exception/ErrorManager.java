package it.gov.pagopa.common.web.exception;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.CustomExceptions.BadRequestException;
import it.gov.pagopa.common.web.exception.CustomExceptions.ForbiddenException;
import it.gov.pagopa.common.web.exception.CustomExceptions.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ErrorManager {
  private final ErrorDTO defaultErrorDTO;

  public ErrorManager(@Nullable ErrorDTO defaultErrorDTO) {
    this.defaultErrorDTO = Optional.ofNullable(defaultErrorDTO)
        .orElse(new ErrorDTO("Error", "Something gone wrong"));
  }

  @ExceptionHandler(RuntimeException.class)
  protected ResponseEntity<ErrorDTO> handleException(RuntimeException error, HttpServletRequest request) {
    if(!(error instanceof ClientException clientException) || clientException.isPrintStackTrace() || clientException.getCause() != null){
      log.error("Something went wrong handling request {}", getRequestDetails(request), error);
    } else {
      log.info("A {} occurred handling request {}: HttpStatus {} - {} at {}",
          clientException.getClass().getSimpleName(),
          getRequestDetails(request),
          clientException.getHttpStatus(),
          clientException.getMessage(),
          clientException.getStackTrace().length > 0 ? clientException.getStackTrace()[0] : "UNKNOWN");
    }

    if(error instanceof ClientExceptionNoBody clientExceptionNoBody){
      return ResponseEntity.status(clientExceptionNoBody.getHttpStatus()).build();
    }
    else {
      ErrorDTO errorDTO;
      HttpStatus httpStatus;
      if (error instanceof ClientExceptionWithBody clientExceptionWithBody){
        httpStatus = clientExceptionWithBody.getHttpStatus();
        errorDTO = new ErrorDTO(clientExceptionWithBody.getCode(),  error.getMessage());
      }
      else {
        httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        errorDTO = defaultErrorDTO;
      }
      return ResponseEntity.status(httpStatus)
          .contentType(MediaType.APPLICATION_JSON)
          .body(errorDTO);
    }
  }

  @ExceptionHandler(BadRequestException.class)
  protected ResponseEntity<ErrorDTO> handleBadRequestException(BadRequestException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ErrorDTO(ex.getCode(), ex.getMessage()));
  }

  @ExceptionHandler(ForbiddenException.class)
  protected ResponseEntity<ErrorDTO> handleForbiddenException(ForbiddenException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ErrorDTO(ex.getCode(), ex.getMessage()));
  }

  @ExceptionHandler(NotFoundException.class)
  protected ResponseEntity<ErrorDTO> handleNotFoundException(NotFoundException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ErrorDTO(ex.getCode(), ex.getMessage()));
  }



  public static String getRequestDetails(HttpServletRequest request) {
    return "%s %s".formatted(request.getMethod(), request.getRequestURI());
  }

}
