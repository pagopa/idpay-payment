package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.event.QueueCommandOperationDTO;
import org.springframework.messaging.Message;

public interface ProcessConsumerService {
    void processCommand(QueueCommandOperationDTO queueCommandOperationDTO);
    void timeoutConsumer(Message<String> message);
}
