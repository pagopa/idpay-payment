package it.gov.pagopa.payment.service.payment.expired.barcode;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.barcode.expired.BarCodeAuthorizationExpiredServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarCodeAuthorizationExpiredServiceImplTest {
    private static final long AUTHORIZATION_EXPIRATION_MINUTES = 5L;
    private static final String TRX_CODE = "trx_code";
    private static final String INITIATIVE_ID = "INITIATIVE_1";

    @Mock
    private TransactionRepository transactionRepositoryMock;
    @Mock
    private RewardCalculatorConnector rewardCalculatorConnectorMock;
    @Mock
    private AuditUtilities auditUtilitiesMock;

    private BarCodeAuthorizationExpiredServiceImpl barCodeAuthorizationExpiredService;

    @BeforeEach
    void setUp() {
        barCodeAuthorizationExpiredService = new BarCodeAuthorizationExpiredServiceImpl(
                transactionRepositoryMock,
                AUTHORIZATION_EXPIRATION_MINUTES,
                rewardCalculatorConnectorMock,
                auditUtilitiesMock
        );
    }

    @Test
    void testFindByTrxCodeAndInitiativeId_Success() {
        Transaction transaction = new Transaction();
        transaction.setTrxCode(TRX_CODE);
        transaction.setInitiativeId(INITIATIVE_ID);

        when(transactionRepositoryMock.findByTrxCodeAndInitiativeIdAndTrxEndDateGreaterThanEqualAndStatusNot(
                eq(TRX_CODE),
                eq(INITIATIVE_ID),
                any(OffsetDateTime.class),
                eq(SyncTrxStatus.CANCELLED)
        )).thenReturn(Optional.of(transaction));

        Transaction result = barCodeAuthorizationExpiredService
                .findByTrxCodeAndTrxEndDateGreaterThanEqualAndStatusNotAndInitiativeId(TRX_CODE, INITIATIVE_ID);

        assertNotNull(result);
        assertEquals(TRX_CODE, result.getTrxCode());
        assertEquals(INITIATIVE_ID, result.getInitiativeId());
        verify(transactionRepositoryMock, times(1))
                .findByTrxCodeAndInitiativeIdAndTrxEndDateGreaterThanEqualAndStatusNot(
                        eq(TRX_CODE),
                        eq(INITIATIVE_ID),
                        any(OffsetDateTime.class),
                        eq(SyncTrxStatus.CANCELLED));
    }

    @Test
    void testFindByTrxCodeAndInitiativeId_NotFound_ThrowsException() {
        when(transactionRepositoryMock.findByTrxCodeAndInitiativeIdAndTrxEndDateGreaterThanEqualAndStatusNot(
                eq(TRX_CODE),
                eq(INITIATIVE_ID),
                any(OffsetDateTime.class),
                eq(SyncTrxStatus.CANCELLED)
        )).thenReturn(Optional.empty());

        TransactionNotFoundOrExpiredException exception = assertThrows(
                TransactionNotFoundOrExpiredException.class,
                () -> barCodeAuthorizationExpiredService
                        .findByTrxCodeAndTrxEndDateGreaterThanEqualAndStatusNotAndInitiativeId(TRX_CODE, INITIATIVE_ID)
        );

        assertTrue(exception.getMessage().contains("Cannot find transaction with trxCode"));
        verify(transactionRepositoryMock, times(1))
                .findByTrxCodeAndInitiativeIdAndTrxEndDateGreaterThanEqualAndStatusNot(
                        eq(TRX_CODE),
                        eq(INITIATIVE_ID),
                        any(OffsetDateTime.class),
                        eq(SyncTrxStatus.CANCELLED));
    }
}
