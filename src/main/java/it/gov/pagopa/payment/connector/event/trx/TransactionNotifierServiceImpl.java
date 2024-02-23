package it.gov.pagopa.payment.connector.event.trx;

import it.gov.pagopa.payment.model.TransactionInProgress;
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
public class TransactionNotifierServiceImpl implements TransactionNotifierService {

    private String binder;

    private final StreamBridge streamBridge;

    public TransactionNotifierServiceImpl(StreamBridge streamBridge,@Value("${spring.cloud.stream.bindings.transactionOutcome-out-0.binder}") String binder ) {
        this.streamBridge = streamBridge;
        this.binder=binder;
    }

    @Configuration
    static class TransactionNotifierServiceImplConfig {
        @Bean
        public Supplier<Flux<Message<Object>>> transactionOutcome() {
            return Flux::empty;
        }
    }

    @Override
    public boolean notify(TransactionInProgress trx, String key) {
       return streamBridge.send("transactionOutcome-out-0", binder, buildMessage(trx, key));
    }

    @Override
    public Message<TransactionInProgress> buildMessage(TransactionInProgress trx, String key) {
        return MessageBuilder.withPayload(trx)
                .setHeader(KafkaHeaders.KEY, key)
                .build();
    }
}
