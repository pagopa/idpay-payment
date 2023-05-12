package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionOutcomeDTOMapper;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.model.TransactionOutcomeDTO;
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

    @Value("${spring.cloud.stream.bindings.transactionOutcome-out-0.binder}")
    private String binder;

    private final StreamBridge streamBridge;
    private final TransactionInProgress2TransactionOutcomeDTOMapper mapper;

    public TransactionNotifierServiceImpl(StreamBridge streamBridge,
        TransactionInProgress2TransactionOutcomeDTOMapper mapper) {
        this.streamBridge = streamBridge;
        this.mapper = mapper;
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
       return streamBridge.send("transactionOutcome-out-0", binder, buildMessage(mapper.apply(trx), key));
    }

    public static Message<TransactionOutcomeDTO> buildMessage(TransactionOutcomeDTO trx, String key) {
        return MessageBuilder.withPayload(trx)
                .setHeader(KafkaHeaders.KEY, key)
                .build();
    }
}
