package it.gov.pagopa.payment.service;

import org.springframework.messaging.Message;

public interface AuthorizationRequestTimeoutService {
    void execute(Message<String> message);
}
