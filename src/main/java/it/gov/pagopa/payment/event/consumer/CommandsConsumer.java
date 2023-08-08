package it.gov.pagopa.payment.event.consumer;

import it.gov.pagopa.payment.dto.event.QueueCommandOperationDTO;
import it.gov.pagopa.payment.service.ProcessConsumerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;


@Configuration
public class CommandsConsumer {
    @Bean
    public Consumer<QueueCommandOperationDTO> consumerCommands(ProcessConsumerService processConsumerService) {
        return processConsumerService::processCommand;
    }
}
