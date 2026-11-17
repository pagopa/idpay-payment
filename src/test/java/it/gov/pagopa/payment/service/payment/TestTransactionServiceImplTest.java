package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.repository.TestTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestTransactionServiceImplTest {

    private static final String TRANSACTION_ID = "trx-id";
    private static final String INITIATIVE_ID = "initiative-id";
    private static final String USER_ID = "user-id";
    private static final OffsetDateTime NEW_END_DATE = OffsetDateTime.parse("2026-11-20T23:59:59+01:00");

    @Mock
    private TestTransactionRepository testTransactionRepository;

    private TestTransactionService service;

    @BeforeEach
    void setUp() {
        service = new TestTransactionServiceImpl(testTransactionRepository);
    }

    @Test
    void shouldUpdateMatchingTransactionEndDate() {
        when(testTransactionRepository.updateTrxEndDate(TRANSACTION_ID, INITIATIVE_ID, USER_ID, NEW_END_DATE))
                .thenReturn(1);

        assertDoesNotThrow(() -> service.updateTrxEndDate(TRANSACTION_ID, INITIATIVE_ID, USER_ID, NEW_END_DATE));

        verify(testTransactionRepository).updateTrxEndDate(TRANSACTION_ID, INITIATIVE_ID, USER_ID, NEW_END_DATE);
    }

    @Test
    void shouldThrowNotFoundWhenNoTransactionMatches() {
        when(testTransactionRepository.updateTrxEndDate(TRANSACTION_ID, INITIATIVE_ID, USER_ID, NEW_END_DATE))
                .thenReturn(0);

        ClientExceptionNoBody exception = assertThrows(ClientExceptionNoBody.class,
                () -> service.updateTrxEndDate(TRANSACTION_ID, INITIATIVE_ID, USER_ID, NEW_END_DATE));

        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        assertEquals("Transaction not found", exception.getMessage());
        verify(testTransactionRepository).updateTrxEndDate(TRANSACTION_ID, INITIATIVE_ID, USER_ID, NEW_END_DATE);
    }

    @Test
    void shouldPropagateRepositoryFailure() {
        DataAccessResourceFailureException repositoryException =
                new DataAccessResourceFailureException("Database unavailable");
        when(testTransactionRepository.updateTrxEndDate(TRANSACTION_ID, INITIATIVE_ID, USER_ID, NEW_END_DATE))
                .thenThrow(repositoryException);

        DataAccessResourceFailureException exception = assertThrows(DataAccessResourceFailureException.class,
                () -> service.updateTrxEndDate(TRANSACTION_ID, INITIATIVE_ID, USER_ID, NEW_END_DATE));

        assertSame(repositoryException, exception);
        verify(testTransactionRepository).updateTrxEndDate(TRANSACTION_ID, INITIATIVE_ID, USER_ID, NEW_END_DATE);
    }
}
