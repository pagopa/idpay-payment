package it.gov.pagopa.payment.event.consumer;

import it.gov.pagopa.payment.service.TimeoutService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;
@Slf4j
@Configuration
public class TimeoutConsumer {
    private final TimeoutService timeoutService;

    public TimeoutConsumer(TimeoutService timeoutService) {
        this.timeoutService = timeoutService;
    }


    @Bean
    public Consumer<Message<String>> paymentTimeoutConsumer() {
        return timeoutService::timeoutConsumer;
    }
}
