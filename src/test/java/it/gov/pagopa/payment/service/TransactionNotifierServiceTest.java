package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierServiceImpl;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

@ExtendWith(MockitoExtension.class)
class TransactionNotifierServiceTest {

    private final String BINDER ="transaction-outcome";
    @Mock
    private StreamBridge streamBridgeMock;

    private TransactionNotifierService service;

    @BeforeEach
    void init() {

        service = new TransactionNotifierServiceImpl(streamBridgeMock,BINDER);
    }

    @Test
    void test(){
        // Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        // When
        boolean result = service.notify(trx,"TEST");

        Mockito.verify(streamBridgeMock).send(Mockito.eq("transactionOutcome-out-0"), Mockito.eq(BINDER), Mockito.<Message<TransactionInProgress>>argThat(m -> m.getPayload().equals(trx)
        ));

        // Then
        Assertions.assertFalse(result);

    }


}


