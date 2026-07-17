package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.service.TransactionService;
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
import java.util.List;

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
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        Long amount = 1000L;
        Pageable pageable = PageRequest.of(0, 10);
        List<Transaction> expectedTransactions = List.of(new Transaction());

        // Simuliamo il comportamento del service
        when(transactionService.findAll(
                ID_TRX_ISSUER,
                USER_ID,
                start,
                end,
                amount,
                pageable
        )).thenReturn(expectedTransactions);

        // Mocking statico di Utilities (se necessario, altrimenti esegue il codice reale di Utilities)
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

            // Verifichiamo che il service sia stato chiamato con i parametri corretti (e sanitizzati)
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
}