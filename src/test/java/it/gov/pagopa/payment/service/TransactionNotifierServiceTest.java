package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierServiceImpl;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

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
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        // When
        boolean result = service.notify(trx,"TEST");

        verify(streamBridgeMock).send(eq("transactionOutcome-out-0"), eq(BINDER), argThat((Message<Transaction> m) -> m.getPayload().equals(trx)
        ));

        // Then
        Assertions.assertFalse(result);

    }


}

