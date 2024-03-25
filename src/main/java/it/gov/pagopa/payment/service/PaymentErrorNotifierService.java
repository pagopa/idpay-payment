package it.gov.pagopa.payment.service;

import it.gov.pagopa.common.kafka.service.ErrorNotifierInfoDTO;
import org.springframework.messaging.Message;

public interface PaymentErrorNotifierService {
    boolean notifyAuthPayment(Message<?> message, String description, boolean retryable, Throwable exception);
    boolean notifyConfirmPayment(Message<?> message, String description, boolean retryable, Throwable exception);
    boolean notifyCancelPayment(Message<?> message, String description, boolean retryable, Throwable exception);
    boolean notify(ErrorNotifierInfoDTO errorNotifierInfoDTO);
}
