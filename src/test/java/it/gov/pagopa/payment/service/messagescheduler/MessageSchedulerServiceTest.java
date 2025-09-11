package it.gov.pagopa.payment.service.messagescheduler;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
class MessageSchedulerServiceTest {

    private MessageSchedulerService messageSchedulerService;
    @MockBean
    private ServiceBusSenderClient serviceBusSenderClientMock;

    @BeforeEach
    void setUp() {
        messageSchedulerService =
                new MessageSchedulerServiceImpl(serviceBusSenderClientMock);
    }
    @Test
    void scheduleMessage(){
        when(serviceBusSenderClientMock.scheduleMessage(Mockito.any(), Mockito.any())).thenReturn(1L);
        messageSchedulerService.scheduleMessage(new ServiceBusMessage("TEST"), OffsetDateTime.now());

        verify(serviceBusSenderClientMock, times(1)).scheduleMessage(Mockito.any(), Mockito.any());
    }

    @Test
    void cancelMessage(){
        doNothing().when(serviceBusSenderClientMock).cancelScheduledMessage(1L);
        messageSchedulerService.cancelScheduledMessage(1L);

        verify(serviceBusSenderClientMock, times(1)).cancelScheduledMessage(1L);
    }
}
