package it.gov.pagopa.payment.service.payment.expired;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.common.CommonConfirmServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QRCodeCancelExpiredServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionInProgressRepository transactionInProgressRepository;

    @Mock
    private AuditUtilities auditUtilities;

    @Mock
    private CommonConfirmServiceImpl commonConfirmService;

    private QRCodeCancelExpiredServiceImpl service;

    private static final long CANCEL_EXPIRATION_MINUTES = 15L;

    @BeforeEach
    void setUp() {
        service = new QRCodeCancelExpiredServiceImpl(
                CANCEL_EXPIRATION_MINUTES,
                transactionRepository,
                transactionInProgressRepository,
                auditUtilities,
                commonConfirmService
        );
    }

    @Test
    void shouldReturnConfiguredExpirationMinutes() {
        assertEquals(
                CANCEL_EXPIRATION_MINUTES,
                service.getExpirationMinutes()
        );
    }

    @Test
    void shouldFindExpiredTransaction() {
        // Given
        String initiativeId = "INITIATIVE_1";

        TransactionInProgress expectedTransaction = new TransactionInProgress();

        when(transactionInProgressRepository.findCancelExpiredTransaction(
                initiativeId,
                CANCEL_EXPIRATION_MINUTES))
                .thenReturn(expectedTransaction);

        // When
        TransactionInProgress result =
                service.findExpiredTransaction(
                        initiativeId,
                        CANCEL_EXPIRATION_MINUTES);

        // Then
        assertEquals(expectedTransaction, result);

        verify(transactionRepository).findAndModifyExpiredTransaction(
                any(OffsetDateTime.class),
                eq(List.of(SyncTrxStatus.AUTHORIZED.name())),
                eq(initiativeId),
                eq(1000)
        );

        verify(transactionInProgressRepository)
                .findCancelExpiredTransaction(
                        initiativeId,
                        CANCEL_EXPIRATION_MINUTES);
    }

    @Test
    void shouldCallConfirmAuthorizedPaymentWhenHandlingExpiredTransaction() {
        // Given
        TransactionInProgress trx = new TransactionInProgress();

        // When
        TransactionInProgress result =
                service.handleExpiredTransaction(trx);

        // Then
        assertEquals(trx, result);

        verify(commonConfirmService)
                .confirmAuthorizedPayment(trx);
    }

    @Test
    void shouldReturnCorrectFlowName() {
        assertEquals(
                "TRANSACTION_CANCEL_EXPIRED",
                service.getFlowName()
        );
    }

    @Test
    void shouldSearchOnlyAuthorizedTransactions() {
        // Given
        String initiativeId = "INITIATIVE";

        when(transactionInProgressRepository.findCancelExpiredTransaction(
                any(),
                anyLong()))
                .thenReturn(null);

        // When
        service.findExpiredTransaction(
                initiativeId,
                CANCEL_EXPIRATION_MINUTES);

        // Then
        ArgumentCaptor<List<String>> statusCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(transactionRepository)
                .findAndModifyExpiredTransaction(
                        any(OffsetDateTime.class),
                        statusCaptor.capture(),
                        eq(initiativeId),
                        eq(1000)
                );

        assertEquals(
                List.of(SyncTrxStatus.AUTHORIZED.name()),
                statusCaptor.getValue()
        );
    }
}