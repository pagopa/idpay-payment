package it.gov.pagopa.payment.service.messagescheduler;

import com.azure.messaging.servicebus.ServiceBusMessage;
import it.gov.pagopa.payment.constants.PaymentConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@Slf4j
public class AuthorizationTimeoutSchedulerServiceImpl implements AuthorizationTimeoutSchedulerService {
    private final MessageSchedulerService messageSchedulerService;
    private final Clock clock;
    private final int timeoutSeconds;

    public AuthorizationTimeoutSchedulerServiceImpl(MessageSchedulerService messageSchedulerService,
                                                    Clock clock, @Value("${app.timeoutPayment.seconds}") int timeoutSeconds) {
        this.messageSchedulerService = messageSchedulerService;
        this.clock = clock;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public long scheduleMessage(String body) {
        ServiceBusMessage message = new ServiceBusMessage(body);
        message.getApplicationProperties().put(PaymentConstants.MESSAGE_TOPIC,PaymentConstants.TIMEOUT_PAYMENT);
        log.info("[SCHEDULE_MESSAGE][TIMEOUT_AUTH] Scheduled message with trxId: {}",body);
        return messageSchedulerService.scheduleMessage(message,Instant.now(clock).plusSeconds(timeoutSeconds));
    }

    @Override
    public void cancelScheduledMessage(long sequenceNumber) {
        log.info("[SCHEDULE_MESSAGE][TIMEOUT_AUTH] Cancelling scheduled message with sequence number: {}",sequenceNumber);
        messageSchedulerService.cancelScheduledMessage(sequenceNumber);
    }
}
