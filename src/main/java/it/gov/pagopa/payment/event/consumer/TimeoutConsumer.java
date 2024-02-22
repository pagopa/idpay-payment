package it.gov.pagopa.payment.event.consumer;

import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.GenericMessage;

import java.util.function.Consumer;
@Slf4j
@Configuration
public class TimeoutConsumer {
    private final TransactionInProgressRepository transactionInProgressRepository;

    public TimeoutConsumer(TransactionInProgressRepository transactionInProgressRepository) {
        this.transactionInProgressRepository = transactionInProgressRepository;
    }

    @Bean
    public Consumer<GenericMessage<String>> paymentTimeoutConsumer() {
        return message -> {
            if (PaymentConstants.TIMEOUT_PAYMENT.equals(message.getHeaders().get("SUBJECT"))) {
                log.info("[TIMEOUT PAYMENT] Start processing transaction with id %s".formatted(message.getPayload()));
                transactionInProgressRepository.updateTrxPostTimeout(message.getPayload());
                log.info("[TIMEOUT PAYMENT] Transaction updated in status REJECTED");
            }
        };
    }
}
