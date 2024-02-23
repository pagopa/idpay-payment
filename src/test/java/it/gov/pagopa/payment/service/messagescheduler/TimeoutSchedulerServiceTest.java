package it.gov.pagopa.payment.service.messagescheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = TimeoutSchedulerServiceImpl.class)
@TestPropertySource(
        locations = "classpath:application.yml",
        properties = {
                "app.timeoutPayment.seconds=30"
        })
class TimeoutSchedulerServiceTest {
    TimeoutSchedulerService timeoutSchedulerService;
    @MockBean
    private MessageSchedulerService messageSchedulerServiceMock;

    @BeforeEach
    void setUp() {
        int timeoutSeconds = 30;
        timeoutSchedulerService =
                new TimeoutSchedulerServiceImpl(messageSchedulerServiceMock, timeoutSeconds);
    }
    @Test
    void scheduleMessage(){
        String body = "TRXID1";

        when(messageSchedulerServiceMock.scheduleMessage(Mockito.any(), Mockito.any())).thenReturn(1L);
        long sequenceNumber = timeoutSchedulerService.scheduleMessage(body);

        assertEquals(1L, sequenceNumber);
    }

    @Test
    void cancelMessage(){
        doNothing().when(messageSchedulerServiceMock).cancelScheduledMessage(1L);
        timeoutSchedulerService.cancelScheduledMessage(1L);

        verify(messageSchedulerServiceMock, times(1)).cancelScheduledMessage(1L);
    }
}
