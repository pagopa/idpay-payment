package it.gov.pagopa.payment.service.payment.expired;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorRestClient;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class QRCodeExpirationServiceImplTest{

    private static final int N = 20;
    private static final int N_EXPIRED = N/2;

    private OffsetDateTime OFFSET_NOW;
    private OffsetDateTime AUTHORIZATION_EXPIRED_DATE;
    private OffsetDateTime CANCEL_EXPIRED_DATE;

    @Mock
    private RewardCalculatorRestClient rewardCalculatorRestClientSpy;

    @Mock
    private QRCodeAuthorizationExpiredService authorizationExpiredService;

    @Mock
    private QRCodeCancelExpiredService cancelExpiredService;

    @Mock
    private TransactionInProgressRepository repository;

    private QRCodeExpirationServiceImpl qrCodeExpirationServiceImpl;

    private final List<TransactionInProgress> trxs = new ArrayList<>(N);
    private final Map<SyncTrxStatus, List<TransactionInProgress>> expiredTrxs = new HashMap<>();
    private final Map<SyncTrxStatus, List<TransactionInProgress>> validTrxs = new HashMap<>();



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


}