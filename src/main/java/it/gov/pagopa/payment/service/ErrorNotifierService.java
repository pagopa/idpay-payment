package it.gov.pagopa.payment.service;

import org.springframework.messaging.Message;

public interface ErrorNotifierService {
    void notifyAuthPayment(Message<?> message, String description, boolean retryable, Throwable exception);
    void notify(String srcType, String srcServer, String srcTopic, String group, Message<?> message, String description, boolean retryable, boolean resendApplication, Throwable exception);
}
