package it.gov.pagopa.payment.service.payment.expired;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorRestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QRCodeExpirationServiceImplTest{
    @Mock
    private RewardCalculatorRestClient rewardCalculatorRestClientSpy;

    @Mock
    private QRCodeAuthorizationExpiredService authorizationExpiredService;

    @Mock
    private QRCodeCancelExpiredService cancelExpiredService;

    @InjectMocks
    private QRCodeExpirationServiceImpl qrCodeExpirationServiceImpl;


    @Test
    void scheduleAuthorizationExpired() {
        // waitFor expired trxs deleted from db
        when(authorizationExpiredService.execute()).thenReturn(1L);
        qrCodeExpirationServiceImpl.scheduleAuthorizationExpired();
        verify(authorizationExpiredService,times(1)).execute();

    }

    @Test
    void forceAuthorizationTrxExpiration() {
        // waitFor expired trxs deleted from db
        when(authorizationExpiredService.forceExpiration("INITIATIVEID")).thenReturn(1L);
        qrCodeExpirationServiceImpl.forceAuthorizationTrxExpiration("INITIATIVEID");
        verify(authorizationExpiredService, times(1)).forceExpiration("INITIATIVEID");

    }
    @Test
    void scheduleCancelExpired() {
        // waitFor expired trxs deleted from db
        when(cancelExpiredService.execute()).thenReturn(1L);
        qrCodeExpirationServiceImpl.scheduleCancelExpired();
        verify(cancelExpiredService, times(1)).execute();

    }

    @Test
    void forceConfirmTrxExpiration() {
        // waitFor expired trxs deleted from db
        when(cancelExpiredService.forceExpiration("INITIATIVEID")).thenReturn(1L);
        qrCodeExpirationServiceImpl.forceConfirmTrxExpiration("INITIATIVEID");
        verify(cancelExpiredService, times(1)).forceExpiration("INITIATIVEID");

    }


}