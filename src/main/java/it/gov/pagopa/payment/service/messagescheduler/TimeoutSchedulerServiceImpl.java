package it.gov.pagopa.payment.service.messagescheduler;

import com.azure.messaging.servicebus.ServiceBusMessage;
import it.gov.pagopa.payment.constants.PaymentConstants;
import org.springframework.beans.factory.annotation.Value;

import java.time.OffsetDateTime;

public class TimeoutSchedulerServiceImpl implements TimeoutSchedulerService {
    private final MessageSchedulerServiceImpl messageSchedulerService;
    @Value("${app.timeoutPayment.seconds}")
    private int timeoutSeconds;

    public TimeoutSchedulerServiceImpl(MessageSchedulerServiceImpl messageSchedulerService) {
        this.messageSchedulerService = messageSchedulerService;
    }

    @Override
    public long scheduleMessage(String body) {
        ServiceBusMessage message = new ServiceBusMessage(body);
        message.getApplicationProperties().put(PaymentConstants.MESSAGE_TOPIC,PaymentConstants.TIMEOUT_PAYMENT);
        return messageSchedulerService.scheduleMessage(message,OffsetDateTime.now().plusSeconds(timeoutSeconds));
    }

    @Override
    public void cancelScheduledMessage(long sequenceNumber) {
        messageSchedulerService.cancelScheduledMessage(sequenceNumber);
    }
}
