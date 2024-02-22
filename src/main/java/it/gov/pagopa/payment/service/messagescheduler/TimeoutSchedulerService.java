package it.gov.pagopa.payment.service.messagescheduler;


public interface TimeoutSchedulerService {

    long scheduleMessage(String body);

    void cancelScheduledMessage(long sequenceNumber);

}
