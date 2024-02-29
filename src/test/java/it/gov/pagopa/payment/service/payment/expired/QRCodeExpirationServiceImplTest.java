package it.gov.pagopa.payment.service.payment.expired;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorRestClient;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QRCodeExpirationServiceImplTest{
    @Mock
    private RewardCalculatorRestClient rewardCalculatorRestClientSpy;

    @Mock
    private QRCodeAuthorizationExpiredService authorizationExpiredService;

    @Mock
    private QRCodeCancelExpiredService cancelExpiredService;

    @Mock
    private TransactionInProgressRepository repository;

    private QRCodeExpirationServiceImpl qrCodeExpirationServiceImpl;

    @BeforeEach
    void init() {
        qrCodeExpirationServiceImpl = new QRCodeExpirationServiceImpl(authorizationExpiredService,cancelExpiredService);
    }

    @Test
    void scheduleAuthorizationExpired() {
        // waitFor expired trxs deleted from db
        Mockito.when(authorizationExpiredService.execute()).thenReturn(1L);
        qrCodeExpirationServiceImpl.scheduleAuthorizationExpired();
        Mockito.verify(authorizationExpiredService,Mockito.times(1)).execute();

    }

    @Test
    void forceAuthorizationTrxExpiration() {
        // waitFor expired trxs deleted from db
        Mockito.when(authorizationExpiredService.forceExpiration("INITIATIVEID")).thenReturn(1L);
        qrCodeExpirationServiceImpl.forceAuthorizationTrxExpiration("INITIATIVEID");
        Mockito.verify(authorizationExpiredService,Mockito.times(1)).forceExpiration("INITIATIVEID");

    }
    @Test
    void scheduleCancelExpired() {
        // waitFor expired trxs deleted from db
        Mockito.when(cancelExpiredService.execute()).thenReturn(1L);
        qrCodeExpirationServiceImpl.scheduleCancelExpired();
        Mockito.verify(cancelExpiredService,Mockito.times(1)).execute();

    }

    @Test
    void forceConfirmTrxExpiration() {
        // waitFor expired trxs deleted from db
        Mockito.when(cancelExpiredService.forceExpiration("INITIATIVEID")).thenReturn(1L);
        qrCodeExpirationServiceImpl.forceConfirmTrxExpiration("INITIATIVEID");
        Mockito.verify(cancelExpiredService,Mockito.times(1)).forceExpiration("INITIATIVEID");

    }


}