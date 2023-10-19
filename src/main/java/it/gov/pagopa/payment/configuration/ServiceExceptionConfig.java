package it.gov.pagopa.payment.configuration;

import it.gov.pagopa.common.web.exception.custom.BadRequestException;
import it.gov.pagopa.common.web.exception.custom.ForbiddenException;
import it.gov.pagopa.common.web.exception.custom.InternalServerErrorException;
import it.gov.pagopa.common.web.exception.custom.NotFoundException;
import it.gov.pagopa.common.web.exception.custom.ServiceException;
import it.gov.pagopa.common.web.exception.custom.TooManyRequestsException;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class ServiceExceptionConfig {

  @Bean
  public Map<Class<? extends ServiceException>, HttpStatus> serviceExceptionMapper(){
    return Map.of(
        BadRequestException.class, HttpStatus.BAD_REQUEST,
        NotFoundException.class, HttpStatus.NOT_FOUND,
        ForbiddenException.class, HttpStatus.FORBIDDEN,
        InternalServerErrorException.class, HttpStatus.INTERNAL_SERVER_ERROR,
        TooManyRequestsException.class, HttpStatus.TOO_MANY_REQUESTS
    );
  }
}
