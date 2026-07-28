package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InternalServerErrorException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonAuthCodeExpirationTest {

    @Mock
    private AuditUtilities auditUtilities;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RewardCalculatorConnector rewardCalculatorConnector;

    private CommonAuthCodeExpiration service;

    @BeforeEach
    void setUp() {
        service = new CommonAuthCodeExpiration(
                auditUtilities,
                "QRCODE",
                15L,
                transactionRepository,
                rewardCalculatorConnector
        ) {};
    }

    @Test
    void shouldSetExpiredWhenStatusIsNotIdentified() {
        Transaction trx = new Transaction();
        trx.setId("trxId");
        trx.setStatus(SyncTrxStatus.AUTHORIZED);

        Transaction result = service.handleExpiredTransaction(trx);

        assertEquals(SyncTrxStatus.EXPIRED, result.getStatus());
        verifyNoInteractions(rewardCalculatorConnector);
        verify(transactionRepository).save(trx);
    }

    @Test
    void shouldCancelAndSetExpiredWhenStatusIsIdentified() {
        Transaction trx = new Transaction();
        trx.setId("trxId");
        trx.setStatus(SyncTrxStatus.IDENTIFIED);

        Transaction result = service.handleExpiredTransaction(trx);

        assertEquals(SyncTrxStatus.EXPIRED, result.getStatus());
        verify(rewardCalculatorConnector).cancelTransaction(trx);
        verify(transactionRepository).save(trx);
    }

    @Test
    void shouldSetExpiredWhenRewardCalculatorReturnsTransactionNotFound() {
        Transaction trx = new Transaction();
        trx.setId("trxId");
        trx.setStatus(SyncTrxStatus.IDENTIFIED);

        doThrow(new TransactionNotFoundOrExpiredException("not found"))
                .when(rewardCalculatorConnector)
                .cancelTransaction(trx);

        Transaction result = service.handleExpiredTransaction(trx);
        assertEquals(SyncTrxStatus.EXPIRED, result.getStatus());
        verify(transactionRepository).save(trx);
    }

    @Test
    void shouldThrowInternalServerErrorWhenRewardCalculatorFails() {
        Transaction trx = new Transaction();
        trx.setId("trxId");
        trx.setStatus(SyncTrxStatus.IDENTIFIED);

        doThrow(new ServiceException("code", "message"))
                .when(rewardCalculatorConnector)
                .cancelTransaction(trx);

        assertThrows(InternalServerErrorException.class, () -> service.handleExpiredTransaction(trx));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void shouldReturnAuthorizationExpirationFlowName() {
        assertEquals("TRANSACTION_AUTHORIZATION_EXPIRED", service.getFlowName());
    }

    @Test
    void shouldReturnConfiguredExpirationMinutes() {
        assertEquals(15L, service.getExpirationMinutes());
    }

    @Test
    void shouldFindAuthorizationExpiredTransaction() {
        String initiativeId = "INITIATIVE_1";
        Transaction expected = new Transaction();

        when(transactionRepository.findAuthorizationExpiredTransaction(
                anyString(),
                any(OffsetDateTime.class),
                any(),
                anyLong()
        )).thenReturn(expected);

        Transaction result = service.findExpiredTransaction(initiativeId, 15L);
        assertEquals(expected, result);
    }
}
