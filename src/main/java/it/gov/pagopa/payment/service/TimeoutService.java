package it.gov.pagopa.payment.service;

import org.springframework.messaging.Message;

public interface TimeoutService {
    void timeoutConsumer(Message<String> message);
}
