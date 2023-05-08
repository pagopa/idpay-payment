package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

@Component
public class TransactionNotifierServiceImpl implements TransactionNotifierService {

    @Value("${spring.cloud.stream.bindings.transactionOutcome-out-0.binder}")
    private String binder;

    private final StreamBridge streamBridge;

    public TransactionNotifierServiceImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Configuration
    static class TransactionNotifierServiceImplConfig {
        @Bean
        public Supplier<Flux<Message<Object>>> transactionQueue() {
            return Flux::empty;
        }
    }

    @Override
    public boolean notify(TransactionInProgress trx) {
       return streamBridge.send("transactionOutcome-out-0",binder,buildMessage(trx));
    }
    public static Message<TransactionInProgress> buildMessage(TransactionInProgress trx){
        return MessageBuilder.withPayload(trx).build();
    }
}
