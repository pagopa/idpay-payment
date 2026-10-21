package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationRequestTimeoutServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AuthorizationRequestTimeoutServiceImpl service;

    @Test
    void executeAuthorizationHasExpired() {
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZATION_REQUESTED);
        trx.setUserId("USERID1");

        when(transactionRepository.updateTrxPostTimeout(any(), any(), any(), any())).thenReturn(1);

        Message<String> message = MessageBuilder.withPayload(trx.getId())
                .setHeader(PaymentConstants.MESSAGE_TOPIC, PaymentConstants.TIMEOUT_PAYMENT)
                .build();

        assertDoesNotThrow(() -> service.execute(message));

        verify(transactionRepository).updateTrxPostTimeout(any(), any(), any(), any());
    }

    @Test
    void executeAuthorizationCompletedInTime() {
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        trx.setUserId("USERID1");

        when(transactionRepository.updateTrxPostTimeout(any(), any(), any(), any())).thenReturn(1);

        Message<String> message = MessageBuilder.withPayload(trx.getId())
                .setHeader(PaymentConstants.MESSAGE_TOPIC, PaymentConstants.TIMEOUT_PAYMENT)
                .build();

        assertDoesNotThrow(() -> service.execute(message));

        verify(transactionRepository).updateTrxPostTimeout(any(), any(), any(), any());
    }

    @Test
    void executeUnhandledMessageTopic() {
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        trx.setUserId("USERID1");

        Message<String> message = MessageBuilder.withPayload(trx.getId())
                .setHeader(PaymentConstants.MESSAGE_TOPIC, "ERROR")
                .build();

        assertDoesNotThrow(() -> service.execute(message));

        // Per un topic non gestito, verifichiamo che NON venga mai chiamato il repository
        verify(transactionRepository, never()).updateTrxPostTimeout(any(), any(), any(), any());
    }
}