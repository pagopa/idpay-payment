package it.gov.pagopa.payment.service.messagescheduler;

import com.azure.messaging.servicebus.ServiceBusMessage;
import it.gov.pagopa.payment.constants.PaymentConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class TimeoutSchedulerServiceImpl implements TimeoutSchedulerService {
    private final MessageSchedulerService messageSchedulerService;
    private final int timeoutSeconds;

    public TimeoutSchedulerServiceImpl(MessageSchedulerService messageSchedulerService,
                                       @Value("${app.timeoutPayment.seconds}") int timeoutSeconds) {
        this.messageSchedulerService = messageSchedulerService;
        this.timeoutSeconds = timeoutSeconds;
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
