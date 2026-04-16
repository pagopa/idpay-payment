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

        OffsetDateTime scheduledUtc = scheduledEnqueueTime.atOffset(ZoneOffset.UTC);

        message.setScheduledEnqueueTime(scheduledUtc);

        return sender.scheduleMessage(message, scheduledUtc);
    }

    @Override
    public void cancelScheduledMessage(long sequenceNumber) {
        sender.cancelScheduledMessage(sequenceNumber);
    }

}
