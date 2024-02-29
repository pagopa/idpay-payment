package it.gov.pagopa.payment.service.messagescheduler;


public interface AuthorizationTimeoutSchedulerService {
    long scheduleMessage(String body);
    void cancelScheduledMessage(long sequenceNumber);

}
