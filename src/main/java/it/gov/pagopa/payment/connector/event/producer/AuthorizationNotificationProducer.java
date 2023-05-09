package it.gov.pagopa.payment.connector.event.producer;

import it.gov.pagopa.payment.connector.event.producer.dto.AuthorizationNotificationDTO;
import it.gov.pagopa.payment.connector.event.producer.mapper.AuthorizationNotificationMapper;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

@Component
public class AuthorizationNotificationProducer {
    @Value("${spring.cloud.stream.bindings.paymentQueue-out-0.binder}")
    private String binder;
    @Autowired
    private StreamBridge streamBridge;
    @Autowired
    private AuthorizationNotificationMapper authorizationNotificationMapper;

    @Configuration
    static class AuthorizationNotificationProducerConfig {
        @Bean
        public Supplier<Flux<Message<Object>>> notificationQueue() {
            return Flux::empty;
        }
    }

    public boolean sendNotification(TransactionInProgress trx, AuthPaymentDTO authPaymentDTO) {
        return streamBridge.send("paymentQueue-out-0", binder, buildMessage(mapperAuthNotification(trx, authPaymentDTO)));
    }

    public AuthorizationNotificationDTO mapperAuthNotification(TransactionInProgress trx, AuthPaymentDTO authPaymentDTO) {
        return authorizationNotificationMapper.map(trx, authPaymentDTO);
    }

    public static Message<AuthorizationNotificationDTO> buildMessage(AuthorizationNotificationDTO notification){
        return MessageBuilder.withPayload(notification)
                .setHeader(KafkaHeaders.KEY, notification.getUserId()).build();
    }
}
