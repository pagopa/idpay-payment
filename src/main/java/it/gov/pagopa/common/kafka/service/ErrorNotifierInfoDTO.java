package it.gov.pagopa.common.kafka.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.messaging.Message;

@Data
@AllArgsConstructor
public class ErrorNotifierInfoDTO {
    private String srcType;
    private String srcServer;
    private String srcTopic;
    private String group;
    private Message<?> message;
    private String description;
    private boolean retryable;
    private boolean resendApplication;
    private Throwable exception;
}
