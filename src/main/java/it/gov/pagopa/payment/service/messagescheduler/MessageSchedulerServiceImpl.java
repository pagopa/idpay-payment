package it.gov.pagopa.payment.service.messagescheduler;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class MessageSchedulerServiceImpl implements MessageSchedulerService {

    private final ServiceBusSenderClient sender;
    public MessageSchedulerServiceImpl(ServiceBusSenderClient sender) {
        this.sender = sender;
    }

    @Override
    public long scheduleMessage(ServiceBusMessage message, OffsetDateTime scheduledEnqueueTime) {
        message.setScheduledEnqueueTime(scheduledEnqueueTime);
        return sender.scheduleMessage(message, scheduledEnqueueTime);
    }

    @Override
    public void cancelScheduledMessage(long sequenceNumber) {
        sender.cancelScheduledMessage(sequenceNumber);
    }

}
