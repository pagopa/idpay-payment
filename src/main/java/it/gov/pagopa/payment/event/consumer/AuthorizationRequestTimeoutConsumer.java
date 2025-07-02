package it.gov.pagopa.payment.event.consumer;

import it.gov.pagopa.payment.service.AuthorizationRequestTimeoutService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;
@Slf4j
@Configuration
public class AuthorizationRequestTimeoutConsumer {
    private final AuthorizationRequestTimeoutService authorizationRequestTimeoutService;

    public AuthorizationRequestTimeoutConsumer(AuthorizationRequestTimeoutService authorizationRequestTimeoutService) {

        this.authorizationRequestTimeoutService = authorizationRequestTimeoutService;

    }

    @Bean
    public Consumer<Message<String>> paymentTimeoutConsumer() {
        return authorizationRequestTimeoutService::execute;
    }
}
