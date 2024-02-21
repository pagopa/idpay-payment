package it.gov.pagopa.payment.event.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;
@Slf4j
@Configuration
public class TimeoutConsumer {
    @Bean
    public Consumer<Message<String>> consume() { 	return a -> log.info(a + "[TIMEOUT-CONSUMER-TEST]"); 	}
}
