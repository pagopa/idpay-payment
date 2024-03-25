package it.gov.pagopa.common.kafka.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.messaging.Message;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
