package it.gov.pagopa.payment.service.messagescheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = AuthorizationTimeoutSchedulerServiceImpl.class)
@TestPropertySource(
        properties = {
                "app.timeoutPayment.seconds"
        })
class AuthorizationTimeoutSchedulerServiceTest {
    AuthorizationTimeoutSchedulerService authorizationTimeoutSchedulerService;
    @MockBean
    private MessageSchedulerService messageSchedulerServiceMock;

    @Value("${app.timeoutPayment.seconds}")
    private int timeoutSeconds;

    @BeforeEach
    void setUp() {
        authorizationTimeoutSchedulerService =
                new AuthorizationTimeoutSchedulerServiceImpl(messageSchedulerServiceMock, timeoutSeconds);
    }

    @Test
    void scheduleMessage(){
        String body = "TRXID1";

        when(messageSchedulerServiceMock.scheduleMessage(Mockito.any(), Mockito.any())).thenReturn(1L);
        long sequenceNumber = authorizationTimeoutSchedulerService.scheduleMessage(body);

        assertEquals(1L, sequenceNumber);
    }

    @Test
    void cancelMessage(){
        doNothing().when(messageSchedulerServiceMock).cancelScheduledMessage(1L);
        authorizationTimeoutSchedulerService.cancelScheduledMessage(1L);

        verify(messageSchedulerServiceMock, times(1)).cancelScheduledMessage(1L);
    }
}
