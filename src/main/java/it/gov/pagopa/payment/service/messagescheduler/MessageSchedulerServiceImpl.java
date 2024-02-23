package it.gov.pagopa.payment.service.messagescheduler;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Slf4j
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
        log.info("Cancelling scheduled message with sequence number: {}",sequenceNumber);
        sender.cancelScheduledMessage(sequenceNumber);
    }

}
