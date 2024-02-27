package it.gov.pagopa.payment.stream;


import it.gov.pagopa.common.stream.service.ErrorPublisherImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErrorPublisherImplTest {
    @Mock
    private StreamBridge streamBridge;

    @InjectMocks
    private ErrorPublisherImpl errorPublisher;

    @Test
    void send() {
        Message<?> mockMessage = mock(Message.class);
        when(streamBridge.send("errors-out-0", mockMessage)).thenReturn(true);
        boolean result = errorPublisher.send(mockMessage);
        verify(streamBridge).send("errors-out-0", mockMessage);
        assertTrue(result); // Assuming it returns true when successful
    }
}
