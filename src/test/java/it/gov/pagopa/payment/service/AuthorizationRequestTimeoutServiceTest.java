package it.gov.pagopa.payment.service;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.bson.BsonString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationRequestTimeoutServiceTest {

    @Mock
    private TransactionInProgressRepository transactionInProgressRepository;

    private AuthorizationRequestTimeoutService service;


    @BeforeEach
    void setUp() {
        service = new AuthorizationRequestTimeoutServiceImpl(transactionInProgressRepository);
    }

    @Test
    void executeAuthorizationHasExpired(){
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZATION_REQUESTED);
        trx.setUserId("USERID1");

        when(transactionInProgressRepository.updateTrxPostTimeout(any())).thenReturn(UpdateResult.acknowledged(0L, 1L, null));

        Message<String> message = MessageBuilder.withPayload(trx.getId())
                                                .setHeader(PaymentConstants.MESSAGE_TOPIC,PaymentConstants.TIMEOUT_PAYMENT)
                                                .build();
        service.execute(message);

        Mockito.verify(transactionInProgressRepository,Mockito.times(1)).updateTrxPostTimeout(trx.getId());

    }

    @Test
    void executeAuthorizationCompletedInTime(){
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        trx.setUserId("USERID1");

        when(transactionInProgressRepository.updateTrxPostTimeout(any())).thenReturn(UpdateResult.acknowledged(0L, 0L, new BsonString(trx.getId())));

        Message<String> message = MessageBuilder.withPayload(trx.getId())
                .setHeader(PaymentConstants.MESSAGE_TOPIC,PaymentConstants.TIMEOUT_PAYMENT)
                .build();
        service.execute(message);

        Mockito.verify(transactionInProgressRepository,Mockito.times(1)).updateTrxPostTimeout(trx.getId());

    }

    @Test
    void executeUnhandledMessageTopic(){
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        trx.setUserId("USERID1");

        Message<String> message = MessageBuilder.withPayload(trx.getId())
                .setHeader(PaymentConstants.MESSAGE_TOPIC,"ERROR")
                .build();
        service.execute(message);

        Mockito.verify(transactionInProgressRepository,Mockito.times(0)).updateTrxPostTimeout(trx.getId());

    }

}
