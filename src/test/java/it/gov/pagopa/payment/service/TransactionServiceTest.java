package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.exception.ClientException;
import it.gov.pagopa.payment.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionInProgressRepository transactionInProgressRepository;

    private TransactionService transactionService;

    private static final String TRANSACTION_ID = "TRANSACTIONID1";

    private static final String USER_ID = "USERID1";
    private static final String DIFFERENT_USER_ID = "USERID2";

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(transactionInProgressRepository);
    }

    @Test
    void getTransaction() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1);
        trx.setUserId(USER_ID);

        when(transactionInProgressRepository.findById(TRANSACTION_ID)).thenReturn(trx);

        TransactionInProgress result = transactionService.getTransaction(TRANSACTION_ID, USER_ID);

        Assertions.assertEquals(trx, result);
    }

    @Test
    void getTransactionNotFound() {
        when(transactionInProgressRepository.findById(Mockito.anyString())).thenReturn(null);

        ClientException result = Assertions.assertThrows(ClientException.class, () ->
                transactionService.getTransaction(TRANSACTION_ID, USER_ID)
        );

        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getHttpStatus());
        Assertions.assertEquals("NOT FOUND", ((ClientExceptionNoBody) result).getMessage());
    }

    @Test
    void getTransactionForbidden() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(2);
        trx.setUserId(DIFFERENT_USER_ID);

        when(transactionInProgressRepository.findById(Mockito.anyString())).thenReturn(trx);

        ClientException result = Assertions.assertThrows(ClientException.class, () ->
                transactionService.getTransaction(TRANSACTION_ID, USER_ID)
        );

        Assertions.assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());
        Assertions.assertEquals("FORBIDDEN", ((ClientExceptionNoBody) result).getMessage());
    }
}
