package it.gov.pagopa.payment.service.messagescheduler;

import com.azure.messaging.servicebus.ServiceBusMessage;

import java.time.OffsetDateTime;

public interface MessageSchedulerService {
    long scheduleMessage(ServiceBusMessage message, OffsetDateTime scheduledEnqueueTime);
    void cancelScheduledMessage(long sequenceNumber);

}
