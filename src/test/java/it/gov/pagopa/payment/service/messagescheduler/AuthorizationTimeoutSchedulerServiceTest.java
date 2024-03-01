package it.gov.pagopa.payment.service.messagescheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationTimeoutSchedulerServiceTest {
    private AuthorizationTimeoutSchedulerService authorizationTimeoutSchedulerService;
    @Mock
    private MessageSchedulerService messageSchedulerServiceMock;

    private final static int TIMEOUTSECONDS = 30;

    @BeforeEach
    void setUp() {
        authorizationTimeoutSchedulerService =
                new AuthorizationTimeoutSchedulerServiceImpl(messageSchedulerServiceMock, TIMEOUTSECONDS);
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
