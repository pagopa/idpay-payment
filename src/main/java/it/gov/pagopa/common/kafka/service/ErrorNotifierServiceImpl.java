package it.gov.pagopa.common.kafka.service;

import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class ErrorNotifierServiceImpl implements ErrorNotifierService {

    private final ErrorPublisher errorPublisher;
    private final String applicationName;

    public ErrorNotifierServiceImpl(
            @Value("${spring.application.name}") String applicationName,

            ErrorPublisher errorPublisher) {
        this.errorPublisher = errorPublisher;
        this.applicationName = applicationName;
    }

    @Override
    public boolean notify(ErrorNotifierInfoDTO errorNotifierInfoDTO) {
        log.info("[ERROR_NOTIFIER] notifying error: {}", errorNotifierInfoDTO.getDescription(), errorNotifierInfoDTO.getException());
        final MessageBuilder<?> errorMessage = MessageBuilder.fromMessage(errorNotifierInfoDTO.getMessage())
                .setHeader(KafkaConstants.ERROR_MSG_HEADER_SRC_TYPE, errorNotifierInfoDTO.getSrcType())
                .setHeader(KafkaConstants.ERROR_MSG_HEADER_SRC_SERVER, errorNotifierInfoDTO.getSrcServer())
                .setHeader(KafkaConstants.ERROR_MSG_HEADER_SRC_TOPIC, errorNotifierInfoDTO.getSrcTopic())
                .setHeader(KafkaConstants.ERROR_MSG_HEADER_DESCRIPTION, errorNotifierInfoDTO.getDescription())
                .setHeader(KafkaConstants.ERROR_MSG_HEADER_RETRYABLE, errorNotifierInfoDTO.isRetryable())
                .setHeader(KafkaConstants.ERROR_MSG_HEADER_STACKTRACE, ExceptionUtils.getStackTrace(errorNotifierInfoDTO.getException()));

        addExceptionInfo(errorMessage, "rootCause", ExceptionUtils.getRootCause(errorNotifierInfoDTO.getException()));
        addExceptionInfo(errorMessage, "cause", errorNotifierInfoDTO.getException().getCause());

        byte[] receivedKey = errorNotifierInfoDTO.getMessage().getHeaders().get(KafkaHeaders.RECEIVED_KEY, byte[].class);
        if(receivedKey!=null){
            errorMessage.setHeader(KafkaHeaders.KEY, new String(receivedKey, StandardCharsets.UTF_8));
        }

        if (errorNotifierInfoDTO.isResendApplication()){
            errorMessage.setHeader(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME, applicationName);
            errorMessage.setHeader(KafkaConstants.ERROR_MSG_HEADER_GROUP, errorNotifierInfoDTO.getGroup());
        }

        if (!errorPublisher.send(errorMessage.build())) {
            log.error("[ERROR_NOTIFIER] Something gone wrong while notifying error");
            return false;
        } else {
            return true;
        }
    }

    private void addExceptionInfo(MessageBuilder<?> errorMessage, String exceptionHeaderPrefix, Throwable rootCause) {
        errorMessage
                .setHeader("%sClass".formatted(exceptionHeaderPrefix), rootCause != null ? rootCause.getClass().getName() : null)
                .setHeader("%sMessage".formatted(exceptionHeaderPrefix), rootCause != null ? rootCause.getMessage() : null);
    }
}
