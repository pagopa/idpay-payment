package it.gov.pagopa.payment.service.messagescheduler;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class MessageSchedulerServiceImpl implements MessageSchedulerService {

    private final ServiceBusSenderClient sender;
    public MessageSchedulerServiceImpl(ServiceBusSenderClient sender) {
        this.sender = sender;
    }


    @Override
    public long scheduleMessage(ServiceBusMessage message, Instant scheduledEnqueueTime) {

        OffsetDateTime scheduledEnqueueUtc =
                OffsetDateTime.ofInstant(scheduledEnqueueTime, ZoneOffset.UTC);

        message.setScheduledEnqueueTime(scheduledEnqueueUtc);

        return sender.scheduleMessage(message, scheduledEnqueueUtc);
    }


    @Override
    public void cancelScheduledMessage(long sequenceNumber) {
        sender.cancelScheduledMessage(sequenceNumber);
    }

}
