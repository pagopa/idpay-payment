package it.gov.pagopa.payment.service;

import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaymentErrorNotifierServiceImpl implements PaymentErrorNotifierService {

    private final ErrorNotifierService errorNotifierService;

    private final String transactionOutcomeMessagingServiceType;
    private final String transactionOutcomeServer;
    private final String transactionOutcomeTopic;

    public PaymentErrorNotifierServiceImpl(ErrorNotifierService errorNotifierService,

                                           @Value("${spring.cloud.stream.binders.transaction-outcome.type}") String transactionOutcomeMessagingServiceType,
                                           @Value("${spring.cloud.stream.binders.transaction-outcome.environment.spring.cloud.stream.kafka.binder.brokers}") String transactionOutcomeServer,
                                           @Value("${spring.cloud.stream.bindings.transactionOutcome-out-0.destination}") String transactionOutcomeTopic) {
        this.errorNotifierService = errorNotifierService;

        this.transactionOutcomeMessagingServiceType= transactionOutcomeMessagingServiceType;
        this.transactionOutcomeServer = transactionOutcomeServer;
        this.transactionOutcomeTopic = transactionOutcomeTopic;
    }

    @Override
    public boolean notifyAuthPayment(Message<?> message, String description, boolean retryable, Throwable exception) {
        return notify(transactionOutcomeMessagingServiceType, transactionOutcomeServer, transactionOutcomeTopic, null, message, description, retryable, false, exception);
    }

    @Override
    public boolean notifyConfirmPayment(Message<?> message, String description, boolean retryable, Throwable exception) {
        return notify(transactionOutcomeMessagingServiceType, transactionOutcomeServer, transactionOutcomeTopic,null,message,description,retryable,false,exception);
    }

    @Override
    public boolean notifyCancelPayment(Message<?> message, String description, boolean retryable, Throwable exception) {
        return notify(transactionOutcomeMessagingServiceType, transactionOutcomeServer, transactionOutcomeTopic, null, message, description, retryable, false, exception);
    }

    @Override
    public boolean notify(String srcType, String srcServer, String srcTopic, String group, Message<?> message, String description, boolean retryable,boolean resendApplication, Throwable exception) {
        return errorNotifierService.notify(srcType, srcServer, srcTopic, group, message, description, retryable,resendApplication, exception);
    }
}
