package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Service
public class TransactionNotifierServiceImpl implements TransactionNotifierService {
    @Value("${spring.cloud.stream.bindings.notificationOutcome-out-0.binder}")
    private String binder;
    private final StreamBridge streamBridge;
    public TransactionNotifierServiceImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }
    @Override
    public boolean notify(TransactionInProgress trx) {
       return streamBridge.send("notificationOutcome-out-0",binder,trx);
    }
}
