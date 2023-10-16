package it.gov.pagopa.common.web.exception;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.custom.BadRequestException;
import it.gov.pagopa.common.web.exception.custom.ForbiddenException;
import it.gov.pagopa.common.web.exception.custom.InternalServerErrorException;
import it.gov.pagopa.common.web.exception.custom.NotFoundException;
import it.gov.pagopa.common.web.exception.custom.ServiceException;
import it.gov.pagopa.common.web.exception.custom.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ServiceExceptionHandler {
  private final ErrorManager errorManager;
  private final Map<Class<? extends ServiceException>, HttpStatus> transcodeMap = Map.of(
      BadRequestException.class, HttpStatus.BAD_REQUEST,
      NotFoundException.class, HttpStatus.NOT_FOUND,
      ForbiddenException.class, HttpStatus.FORBIDDEN,
      InternalServerErrorException.class, HttpStatus.INTERNAL_SERVER_ERROR,
      TooManyRequestsException.class, HttpStatus.TOO_MANY_REQUESTS
  );

  public ServiceExceptionHandler(ErrorManager errorManager) {
    this.errorManager = errorManager;
  }

  @ExceptionHandler(ServiceException.class)
  protected ResponseEntity<ErrorDTO> handleException(ServiceException error, HttpServletRequest request) {
    return errorManager.handleException(transcodeException(error), request);
  }

  private ClientException transcodeException(ServiceException error) {
    HttpStatus httpStatus = transcodeMap.getOrDefault(error.getClass(), HttpStatus.INTERNAL_SERVER_ERROR);
    return new ClientExceptionWithBody(httpStatus, error.getCode(), error.getMessage(), error.getCause());
  }

}
