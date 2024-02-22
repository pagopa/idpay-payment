package it.gov.pagopa.payment.event.consumer;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;
@Slf4j
@Configuration
public class TimeoutConsumer {
    private final TransactionInProgressRepository transactionInProgressRepository;

    public TimeoutConsumer(TransactionInProgressRepository transactionInProgressRepository) {
        this.transactionInProgressRepository = transactionInProgressRepository;
    }

    @Bean
    public Consumer<ServiceBusReceivedMessage> paymentTimeoutConsumer() {
        return message -> {
            if (PaymentConstants.TIMEOUT_PAYMENT.equals(message.getSubject())) {
                log.info("[TIMEOUT PAYMENT] Start processing transaction with id %s".formatted(String.valueOf(message.getBody())));
                transactionInProgressRepository.updateTrxPostTimeout(String.valueOf(message.getBody()));
                log.info("[TIMEOUT PAYMENT] Transaction updated in status REJECTED");
            }
        };
    }
}
