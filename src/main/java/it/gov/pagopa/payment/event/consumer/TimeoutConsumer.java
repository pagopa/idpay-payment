package it.gov.pagopa.payment.event.consumer;

import it.gov.pagopa.payment.service.ProcessConsumerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;
@Slf4j
@Configuration
public class TimeoutConsumer {
    private final ProcessConsumerService processConsumerService;

    public TimeoutConsumer(ProcessConsumerService processConsumerService) {
        this.processConsumerService = processConsumerService;
    }

    @Bean
    public Consumer<Message<String>> paymentTimeoutConsumer() {
        return processConsumerService::timeoutConsumer;
    }
}
