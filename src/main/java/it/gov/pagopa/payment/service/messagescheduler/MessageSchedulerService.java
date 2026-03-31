package it.gov.pagopa.payment.service.messagescheduler;

import com.azure.messaging.servicebus.ServiceBusMessage;

import java.time.Instant;

public interface MessageSchedulerService {
    long scheduleMessage(ServiceBusMessage message, Instant scheduledEnqueueTime);
    void cancelScheduledMessage(long sequenceNumber);

}
