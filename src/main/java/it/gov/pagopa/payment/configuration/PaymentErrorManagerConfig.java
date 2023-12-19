package it.gov.pagopa.payment.configuration;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;

@Configuration
public class PaymentErrorManagerConfig {
  @Bean
  ErrorDTO defaultErrorDTO() {
    return new ErrorDTO(
            ExceptionCode.GENERIC_ERROR,
            "A generic error occurred"
    );
  }

  @Bean
  ErrorDTO tooManyRequestsErrorDTO() {
    return new ErrorDTO(ExceptionCode.TOO_MANY_REQUESTS, "Too Many Requests");
  }

  @Bean
  ErrorDTO templateValidationErrorDTO() {
    return new ErrorDTO(ExceptionCode.PAYMENT_INVALID_REQUEST,"Invalid request");
  }
}
