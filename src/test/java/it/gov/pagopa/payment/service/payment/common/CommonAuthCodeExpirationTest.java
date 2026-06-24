package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InternalServerErrorException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonAuthCodeExpirationTest {

    @Mock
    private AuditUtilities auditUtilities;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionInProgressRepository transactionInProgressRepository;

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
                transactionInProgressRepository,
                rewardCalculatorConnector
        ) {};
    }

    @Test
    void shouldDeleteTransactionWhenStatusIsNotIdentified() {

        TransactionInProgress trx = new TransactionInProgress();
        trx.setId("trxId");
        trx.setStatus(SyncTrxStatus.AUTHORIZED);

        TransactionInProgress result = service.handleExpiredTransaction(trx);

        assertEquals(trx, result);

        verifyNoInteractions(rewardCalculatorConnector);

        verify(transactionRepository)
                .deleteById("trxId");

        verify(transactionInProgressRepository)
                .deleteById("trxId");
    }

    @Test
    void shouldCancelAndDeleteWhenStatusIsIdentified() {

        TransactionInProgress trx = new TransactionInProgress();
        trx.setId("trxId");
        trx.setStatus(SyncTrxStatus.IDENTIFIED);

        TransactionInProgress result = service.handleExpiredTransaction(trx);

        assertEquals(trx, result);

        verify(rewardCalculatorConnector)
                .cancelTransaction(trx);

        verify(transactionRepository)
                .deleteById("trxId");

        verify(transactionInProgressRepository)
                .deleteById("trxId");
    }

    @Test
    void shouldDeleteWhenRewardCalculatorReturnsTransactionNotFound() {

        TransactionInProgress trx = new TransactionInProgress();
        trx.setId("trxId");
        trx.setStatus(SyncTrxStatus.IDENTIFIED);

        doThrow(new TransactionNotFoundOrExpiredException("not found"))
                .when(rewardCalculatorConnector)
                .cancelTransaction(trx);

        TransactionInProgress result = service.handleExpiredTransaction(trx);

        assertEquals(trx, result);

        verify(transactionRepository)
                .deleteById("trxId");

        verify(transactionInProgressRepository)
                .deleteById("trxId");
    }

    @Test
    void shouldThrowInternalServerErrorWhenRewardCalculatorFails() {

        TransactionInProgress trx = new TransactionInProgress();
        trx.setId("trxId");
        trx.setStatus(SyncTrxStatus.IDENTIFIED);

        doThrow(new ServiceException("code","message"))
                .when(rewardCalculatorConnector)
                .cancelTransaction(trx);

        assertThrows(
                InternalServerErrorException.class,
                () -> service.handleExpiredTransaction(trx)
        );

        verify(transactionRepository, never())
                .deleteById(anyString());

        verify(transactionInProgressRepository, never())
                .deleteById(anyString());
    }

    @Test
    void shouldReturnAuthorizationExpirationFlowName() {
        assertEquals(
                "TRANSACTION_AUTHORIZATION_EXPIRED",
                service.getFlowName()
        );
    }

    @Test
    void shouldReturnConfiguredExpirationMinutes() {
        assertEquals(
                15L,
                service.getExpirationMinutes()
        );
    }

    @Test
    void shouldFindAuthorizationExpiredTransaction() {

        // Given
        String initiativeId = "INITIATIVE_1";

        TransactionInProgress expected = new TransactionInProgress();

        when(transactionInProgressRepository.findAuthorizationExpiredTransaction(
                initiativeId,
                15L))
                .thenReturn(expected);

        // When
        TransactionInProgress result =
                service.findExpiredTransaction(
                        initiativeId,
                        15L);

        // Then
        assertEquals(expected, result);

        verify(transactionRepository)
                .findAuthorizationExpiredTransaction(
                        eq(initiativeId),
                        any(OffsetDateTime.class),
                        eq(List.of("IDENTIFIED", "CREATED", "REJECTED")),
                        eq(1000L)
                );

        verify(transactionInProgressRepository)
                .findAuthorizationExpiredTransaction(
                        initiativeId,
                        15L);
    }
}