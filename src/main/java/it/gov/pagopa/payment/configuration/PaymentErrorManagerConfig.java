package it.gov.pagopa.payment.configuration;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentErrorManagerConfig {

  @Bean
  ErrorDTO defaultErrorDTO() {
    return new ErrorDTO(
        PaymentConstants.ExceptionCode.GENERIC_ERROR,
        "A generic error occurred for payment"
    );
  }
}
