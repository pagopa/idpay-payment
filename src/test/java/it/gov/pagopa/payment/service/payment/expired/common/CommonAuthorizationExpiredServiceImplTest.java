package it.gov.pagopa.payment.service.payment.expired.common;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InternalServerErrorException;
import it.gov.pagopa.payment.exception.custom.TooManyRequestsException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonAuthorizationExpiredServiceImplTest {

    @Mock
    private TransactionRepository transactionRepositoryMock;
    @Mock
    private RewardCalculatorConnector rewardCalculatorConnectorMock;
    @Mock
    private AuditUtilities auditUtilitiesMock;

    private static final long EXPIRATION_MINUTES = 30L;
    private static final String CHANNEL = "QR_CODE";
    private static final String TRX_CODE = "TRX_CODE_123";
    private static final String TRX_ID = "TRX_ID_123";

    private DummyCommonAuthorizationExpiredServiceImpl service;

    // Concrete implementation of abstract class for testing purposes
    private static class DummyCommonAuthorizationExpiredServiceImpl extends CommonAuthorizationExpiredServiceImpl {
        public DummyCommonAuthorizationExpiredServiceImpl(
                TransactionRepository transactionRepository,
                long authorizationExpirationMinutes,
                RewardCalculatorConnector rewardCalculatorConnector,
                AuditUtilities auditUtilities,
                String channel) {
            super(transactionRepository, authorizationExpirationMinutes, rewardCalculatorConnector, auditUtilities, channel);
        }
    }

    @BeforeEach
    void setUp() {
        service = new DummyCommonAuthorizationExpiredServiceImpl(
                transactionRepositoryMock,
                EXPIRATION_MINUTES,
                rewardCalculatorConnectorMock,
                auditUtilitiesMock,
                CHANNEL
        );
    }

    @Test
    void testFindByTrxCodeAndAuthorizationNotExpired_Success() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setId(TRX_ID);
        transaction.setTrxCode(TRX_CODE);

        when(transactionRepositoryMock.findByTrxCodeAndTrxEndDateGreaterThanEqual(eq(TRX_CODE), any(OffsetDateTime.class)))
                .thenReturn(Optional.of(transaction));

        // When
        Transaction result = service.findByTrxCodeAndAuthorizationNotExpired(TRX_CODE);

        // Then
        assertNotNull(result);
        assertEquals(TRX_ID, result.getId());
        verify(transactionRepositoryMock, times(1))
                .findByTrxCodeAndTrxEndDateGreaterThanEqual(eq(TRX_CODE), any(OffsetDateTime.class));
    }

    @Test
    void testFindByTrxCodeAndAuthorizationNotExpired_NotFound() {
        // Given
        when(transactionRepositoryMock.findByTrxCodeAndTrxEndDateGreaterThanEqual(eq(TRX_CODE), any(OffsetDateTime.class)))
                .thenReturn(Optional.empty());

        // When & Then
        TransactionNotFoundOrExpiredException exception = assertThrows(
                TransactionNotFoundOrExpiredException.class,
                () -> service.findByTrxCodeAndAuthorizationNotExpired(TRX_CODE)
        );

        assertTrue(exception.getMessage().contains("Cannot find transaction with trxCode"));
    }

    @Test
    void testFindByTrxCodeAndAuthorizationNotExpiredThrottled_Success() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setId(TRX_ID);
        transaction.setTrxCode(TRX_CODE);

        when(transactionRepositoryMock.findAndModifyThrottled(eq(TRX_CODE), any(OffsetDateTime.class)))
                .thenReturn(Optional.of(transaction));
        when(transactionRepositoryMock.existsByTrxCodeAndTrxDateGreaterThan(eq(TRX_CODE), any(OffsetDateTime.class)))
                .thenReturn(false);

        // When
        Transaction result = service.findByTrxCodeAndAuthorizationNotExpiredThrottled(TRX_CODE);

        // Then
        assertNotNull(result);
        assertEquals(TRX_ID, result.getId());
        verify(transactionRepositoryMock, times(1)).findAndModifyThrottled(eq(TRX_CODE), any(OffsetDateTime.class));
        verify(transactionRepositoryMock, times(1)).existsByTrxCodeAndTrxDateGreaterThan(eq(TRX_CODE), any(OffsetDateTime.class));
    }

    @Test
    void testFindByTrxCodeAndAuthorizationNotExpiredThrottled_NotFound() {
        // Given
        when(transactionRepositoryMock.findAndModifyThrottled(eq(TRX_CODE), any(OffsetDateTime.class)))
                .thenReturn(Optional.empty());

        // When & Then
        TransactionNotFoundOrExpiredException exception = assertThrows(
                TransactionNotFoundOrExpiredException.class,
                () -> service.findByTrxCodeAndAuthorizationNotExpiredThrottled(TRX_CODE)
        );

        assertTrue(exception.getMessage().contains("Cannot find transaction with trxCode"));
        verify(transactionRepositoryMock, never()).existsByTrxCodeAndTrxDateGreaterThan(any(), any());
    }

    @Test
    void testFindByTrxCodeAndAuthorizationNotExpiredThrottled_TooManyRequests() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setId(TRX_ID);

        when(transactionRepositoryMock.findAndModifyThrottled(eq(TRX_CODE), any(OffsetDateTime.class)))
                .thenReturn(Optional.of(transaction));
        when(transactionRepositoryMock.existsByTrxCodeAndTrxDateGreaterThan(eq(TRX_CODE), any(OffsetDateTime.class)))
                .thenReturn(true);

        // When & Then
        TooManyRequestsException exception = assertThrows(
                TooManyRequestsException.class,
                () -> service.findByTrxCodeAndAuthorizationNotExpiredThrottled(TRX_CODE)
        );

        assertTrue(exception.getMessage().contains("Too many requests on trx having trCode"));
    }


    @Test
    void testHandleExpiredTransaction_IdentifiedStatus_TransactionNotFoundOrExpiredExceptionIgnored() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setId(TRX_ID);
        transaction.setStatus(SyncTrxStatus.IDENTIFIED);

        doThrow(new TransactionNotFoundOrExpiredException("Not found"))
                .when(rewardCalculatorConnectorMock).cancelTransaction(transaction);
        when(transactionRepositoryMock.save(transaction)).thenReturn(transaction);

        // When
        Transaction result = service.handleExpiredTransaction(transaction);

        // Then
        assertEquals(SyncTrxStatus.EXPIRED, result.getStatus());
        verify(rewardCalculatorConnectorMock, times(1)).cancelTransaction(transaction);
        verify(transactionRepositoryMock, times(1)).save(transaction);
    }

    @Test
    void testHandleExpiredTransaction_IdentifiedStatus_OtherServiceExceptionThrowsInternalServerError() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setId(TRX_ID);
        transaction.setStatus(SyncTrxStatus.IDENTIFIED);

        ServiceException serviceException = new ServiceException("GENERIC", "Reward error");
        doThrow(serviceException).when(rewardCalculatorConnectorMock).cancelTransaction(transaction);

        // When & Then
        InternalServerErrorException exception = assertThrows(
                InternalServerErrorException.class,
                () -> service.handleExpiredTransaction(transaction)
        );

        assertEquals(PaymentConstants.ExceptionCode.GENERIC_ERROR, exception.getCode());
        assertTrue(exception.getMessage().contains("An error occurred in the microservice reward-calculator"));
        verify(transactionRepositoryMock, never()).save(any());
    }

    @Test
    void testGettersAndFlowName() {
        assertEquals(EXPIRATION_MINUTES, service.getExpirationMinutes());
        assertEquals("TRANSACTION_AUTHORIZATION_EXPIRED", service.getFlowName());
    }
}