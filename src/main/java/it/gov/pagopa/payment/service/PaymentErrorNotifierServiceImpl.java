package it.gov.pagopa.payment.service;

import it.gov.pagopa.common.config.KafkaConfiguration;
import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaymentErrorNotifierServiceImpl implements PaymentErrorNotifierService {
    private static final String BINDING_NAME_TRANSACTION_OUTCOME = "transactionOutcome-out-0";

    private final ErrorNotifierService errorNotifierService;
    private final KafkaConfiguration kafkaConfiguration;

    public PaymentErrorNotifierServiceImpl(ErrorNotifierService errorNotifierService,
                                           KafkaConfiguration kafkaConfiguration) {
        this.errorNotifierService = errorNotifierService;

        this.kafkaConfiguration = kafkaConfiguration;
    }

    @Override
    public boolean notifyAuthPayment(Message<?> message, String description, boolean retryable, Throwable exception) {
        return notify(kafkaConfiguration.getStream().getBindings().get(BINDING_NAME_TRANSACTION_OUTCOME), message, description, retryable, false, exception);
    }

    @Override
    public boolean notifyConfirmPayment(Message<?> message, String description, boolean retryable, Throwable exception) {
        return notify(kafkaConfiguration.getStream().getBindings().get(BINDING_NAME_TRANSACTION_OUTCOME), message, description, retryable, false, exception);
    }

    @Override
    public boolean notifyCancelPayment(Message<?> message, String description, boolean retryable, Throwable exception) {
        return notify(kafkaConfiguration.getStream().getBindings().get(BINDING_NAME_TRANSACTION_OUTCOME), message, description, retryable, false, exception);
    }

    @Override
    public boolean notifyRewardPayment(Message<?> message, String description, boolean retryable, Throwable exception) {
        return notify(kafkaConfiguration.getStream().getBindings().get(BINDING_NAME_TRANSACTION_OUTCOME), message, description, retryable, false, exception);
    }

    @Override
    public boolean notify(KafkaConfiguration.BaseKafkaInfoDTO kafkaInfoDTO, Message<?> message, String description, boolean retryable, boolean resendApplication, Throwable exception) {
        return errorNotifierService.notify(kafkaInfoDTO, message, description, retryable, resendApplication, exception);
    }
}
