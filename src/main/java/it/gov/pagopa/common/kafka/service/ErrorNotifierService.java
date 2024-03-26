package it.gov.pagopa.common.kafka.service;

import it.gov.pagopa.common.config.KafkInfoConfig;
import org.springframework.messaging.Message;

public interface ErrorNotifierService {
    boolean notify(KafkInfoConfig.KafkaInfoDTO kafkaInfoDTO, Message<?> message, String description, boolean retryable, boolean resendApplication, Throwable exception);
}