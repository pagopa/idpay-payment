package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.dto.TransactionDTO;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionMapper;
import it.gov.pagopa.payment.exception.ClientException;
import it.gov.pagopa.payment.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    TransactionInProgressRepository transactionInProgressRepository;

    @Mock
    TransactionInProgress2TransactionMapper transactionInProgress2TransactionMapper;

    TransactionService transactionService;

    private static final String TRANSACTION_ID = "TRANSACTIONID1";

    private static final String USER_ID = "USERID1";

    @BeforeEach
    void setUp() {
        transactionService =
                new TransactionServiceImpl(
                        transactionInProgress2TransactionMapper,
                        transactionInProgressRepository);
    }

    @Test
    void getTransaction() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1);
        TransactionDTO trxDTO = TransactionDTOFaker.mockInstance(1);

        when(transactionInProgressRepository.findById(TRANSACTION_ID)).thenReturn(trx);
        when(transactionInProgress2TransactionMapper.apply(any(TransactionInProgress.class))).thenReturn(trxDTO);

        TransactionDTO result = transactionService.getTransaction(TRANSACTION_ID, USER_ID);

        Assertions.assertInstanceOf(TransactionDTO.class, result);
    }

    @Test
    void getTransactionNotFound() {
        when(transactionInProgressRepository.findById(Mockito.anyString())).thenReturn(null);

        ClientException result = Assertions.assertThrows(ClientException.class, () ->
                transactionService.getTransaction(TRANSACTION_ID, USER_ID)
        );

        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getHttpStatus());
        Assertions.assertEquals("NOT FOUND", ((ClientExceptionWithBody) result).getTitle());
    }

    @Test
    void getTransactionForbidden() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(2);
        when(transactionInProgressRepository.findById(Mockito.anyString())).thenReturn(trx);

        ClientException result = Assertions.assertThrows(ClientException.class, () ->
                transactionService.getTransaction(TRANSACTION_ID, USER_ID)
        );

        Assertions.assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());
        Assertions.assertEquals("FORBIDDEN", ((ClientExceptionWithBody) result).getTitle());
    }
}
