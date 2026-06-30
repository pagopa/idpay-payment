package it.gov.pagopa.common.kafka.service;

import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.entity.TransactionOutbox;
import it.gov.pagopa.payment.repository.TransactionOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TransactionOutboxPublisher {

    private final TransactionOutboxRepository repository;
    private final TransactionNotifierService notifier;

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void publish() {
        List<TransactionOutbox> events =
                repository.findTop100ByPublishedFalseOrderByCreatedAtAsc();

        for (TransactionOutbox event : events) {
            boolean sent = notifier.notify(event, event.getTransactionId());

            if (sent) {
                repository.markAsPublished(event.getId());
            }
        }
    }

}
