package it.gov.pagopa.payment.service;

import org.springframework.messaging.Message;

public interface PaymentErrorNotifierService {
    boolean notifyAuthPayment(Message<?> message, String description, boolean retryable, Throwable exception);
    boolean notifyConfirmPayment(Message<?> message, String description, boolean retryable, Throwable exception);
    boolean notifyCancelPayment(Message<?> message, String description, boolean retryable, Throwable exception);
    @SuppressWarnings("squid:S00107")
    boolean notify(String srcType, String srcServer, String srcTopic, String group, Message<?> message, String description, boolean retryable, boolean resendApplication, Throwable exception);
}
