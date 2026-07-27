package it.gov.pagopa.payment.service.payment.expired.idpaycode;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.idpaycode.expired.IdpayCodeAuthorizationExpiredServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdpayCodeAuthorizationExpiredServiceImplTest {

    @Mock
    private TransactionRepository transactionRepositoryMock;
    @Mock
    private AuditUtilities auditUtilitiesMock;
    @Mock
    private RewardCalculatorConnector rewardCalculatorConnectorMock;

    private IdpayCodeAuthorizationExpiredServiceImpl idpayCodeAuthorizationExpiredService;

    private static final long AUTHORIZATION_EXPIRATION_MINUTES = 5L;
    private static final String TRX_ID = "IDPAY_CODE_TRX_123";

    @BeforeEach
    void setUp() {
        idpayCodeAuthorizationExpiredService = new IdpayCodeAuthorizationExpiredServiceImpl(
                AUTHORIZATION_EXPIRATION_MINUTES,
                transactionRepositoryMock,
                auditUtilitiesMock,
                rewardCalculatorConnectorMock
        );
    }

    @Test
    void testFindByTrxIdAndAuthorizationNotExpired_Success() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setId(TRX_ID);
        transaction.setChannel(RewardConstants.TRX_CHANNEL_IDPAYCODE);

        when(transactionRepositoryMock.findByIdAndTrxDateGreaterThanEqual(eq(TRX_ID), any(LocalDateTime.class)))
                .thenReturn(Optional.of(transaction));

        // When
        Transaction result = idpayCodeAuthorizationExpiredService.findByTrxIdAndAuthorizationNotExpired(TRX_ID);

        // Then
        assertNotNull(result);
        assertEquals(TRX_ID, result.getId());
        assertEquals(RewardConstants.TRX_CHANNEL_IDPAYCODE, result.getChannel());
        verify(transactionRepositoryMock, times(1))
                .findByIdAndTrxDateGreaterThanEqual(eq(TRX_ID), any(LocalDateTime.class));
    }

    @Test
    void testFindByTrxIdAndAuthorizationNotExpired_NotFound_ThrowsException() {
        // Given
        when(transactionRepositoryMock.findByIdAndTrxDateGreaterThanEqual(eq(TRX_ID), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        // When & Then
        TransactionNotFoundOrExpiredException exception = assertThrows(
                TransactionNotFoundOrExpiredException.class,
                () -> idpayCodeAuthorizationExpiredService.findByTrxIdAndAuthorizationNotExpired(TRX_ID)
        );

        assertTrue(exception.getMessage().contains("Cannot find voucher with trxId"));
        verify(transactionRepositoryMock, times(1))
                .findByIdAndTrxDateGreaterThanEqual(eq(TRX_ID), any(LocalDateTime.class));
    }

}