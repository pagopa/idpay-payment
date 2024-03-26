package it.gov.pagopa.payment.service;

import it.gov.pagopa.common.config.KafkaConfiguration;
import org.springframework.messaging.Message;

public interface PaymentErrorNotifierService {
    boolean notifyAuthPayment(Message<?> message, String description, boolean retryable, Throwable exception);
    boolean notifyConfirmPayment(Message<?> message, String description, boolean retryable, Throwable exception);
    boolean notifyCancelPayment(Message<?> message, String description, boolean retryable, Throwable exception);
    boolean notify(KafkaConfiguration.BaseKafkaInfoDTO kafkaInfoDTO, Message<?> message, String description, boolean retryable, boolean resendApplication, Throwable exception);
}
