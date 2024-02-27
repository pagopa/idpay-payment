package it.gov.pagopa.payment.event.consumer;

import it.gov.pagopa.payment.service.AuthorizationRequestTimeoutService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthorizationRequestTimeoutConsumerTest {

    @Mock
    private AuthorizationRequestTimeoutService authorizationRequestTimeoutService;

    @InjectMocks
    private AuthorizationRequestTimeoutConsumer consumer;

    @Test
    void testPaymentTimeoutConsumerNotNull(){
        Consumer<Message<String>> paymentTimeoutConsumer = consumer.paymentTimeoutConsumer();
        assertNotNull(paymentTimeoutConsumer);
    }

    @Test
    void testExecuteMethodCalled(){
        Consumer<Message<String>> paymentTimeoutConsumer = consumer.paymentTimeoutConsumer();
        Message<String> message = MessageBuilder.withPayload("test")
                .setHeader("foo", "bar")
                .build();
        paymentTimeoutConsumer.accept(message);
        verify(authorizationRequestTimeoutService).execute(message);
    }


}
