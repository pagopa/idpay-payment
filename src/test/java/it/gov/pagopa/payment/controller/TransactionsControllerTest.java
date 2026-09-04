package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.UpdateTransactionsStatusRequest;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.service.payment.TransactionService;
import it.gov.pagopa.payment.utils.Utilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionsControllerImplTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionsControllerImpl transactionsController;

    private static final String ID_TRX_ISSUER = "issuer_123";
    private static final String USER_ID = "user_456";
    private static final String INITIATIVE_ID = "initiative_789";

    @Test
    void findAll_shouldSanitizeInputsAndReturnTransactions() {
        // Given
        LocalDateTime start = LocalDateTime.now(ZoneId.of("Europe/Rome")).minusDays(1);
        LocalDateTime end = LocalDateTime.now(ZoneId.of("Europe/Rome"));
        Long amount = 1000L;
        Pageable pageable = PageRequest.of(0, 10);
        List<Transaction> expectedTransactions = List.of(new Transaction());

        when(transactionService.findAll(
                ID_TRX_ISSUER,
                USER_ID,
                start,
                end,
                amount,
                pageable
        )).thenReturn(expectedTransactions);

        try (MockedStatic<Utilities> utilitiesMock = Mockito.mockStatic(Utilities.class)) {
            utilitiesMock.when(() -> Utilities.sanitizeString(ID_TRX_ISSUER)).thenReturn(ID_TRX_ISSUER);
            utilitiesMock.when(() -> Utilities.sanitizeString(USER_ID)).thenReturn(USER_ID);

            // When
            List<Transaction> result = transactionsController.findAll(
                    ID_TRX_ISSUER,
                    USER_ID,
                    start,
                    end,
                    amount,
                    pageable
            );

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(expectedTransactions, result);

            verify(transactionService, times(1)).findAll(
                    ID_TRX_ISSUER,
                    USER_ID,
                    start,
                    end,
                    amount,
                    pageable
            );
        }
    }

    @Test
    void findByInitiativeIdAndUserId_shouldSanitizeInputsAndReturnTransactions() {
        // Given
        List<Transaction> expectedTransactions = List.of(new Transaction());

        when(transactionService.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(expectedTransactions);

        try (MockedStatic<Utilities> utilitiesMock = Mockito.mockStatic(Utilities.class)) {
            utilitiesMock.when(() -> Utilities.sanitizeString(INITIATIVE_ID)).thenReturn(INITIATIVE_ID);
            utilitiesMock.when(() -> Utilities.sanitizeString(USER_ID)).thenReturn(USER_ID);

            // When
            List<Transaction> result = transactionsController.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(expectedTransactions, result);

            verify(transactionService, times(1)).findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID);
        }
    }

    @Test
    void updateTransactionsStatus_shouldSanitizeIdsAndDelegateToService() {
        // Given
        UpdateTransactionsStatusRequest request = new UpdateTransactionsStatusRequest(
                Set.of(" trx-1 ", "trx-2"),
                SyncTrxStatus.REWARDED
        );

        when(transactionService.updateTransactionsStatus(anyList(), eq(SyncTrxStatus.REWARDED)))
                .thenReturn(2);

        try (MockedStatic<Utilities> utilitiesMock = Mockito.mockStatic(Utilities.class)) {
            utilitiesMock.when(() -> Utilities.sanitizeString(" trx-1 ")).thenReturn(" trx-1 ");
            utilitiesMock.when(() -> Utilities.sanitizeString("trx-2")).thenReturn("trx-2");
            // When
            int updated = transactionsController.updateTransactionsStatus(request);

            // Then
            assertEquals(2, updated);
            verify(transactionService, times(1))
                    .updateTransactionsStatus(
                            argThat(ids -> ids.size() == 2 && ids.containsAll(List.of("trx-1", "trx-2"))),
                            eq(SyncTrxStatus.REWARDED)
                    );
        }
    }
}