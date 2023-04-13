package it.gov.pagopa.payment.service;

import static org.mockito.Mockito.when;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.ClientException;
import it.gov.pagopa.payment.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

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
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1,
        SyncTrxStatus.IDENTIFIED);
    trx.setUserId(USER_ID);

    when(transactionInProgressRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));

    TransactionInProgress result = transactionService.getTransaction(TRANSACTION_ID, USER_ID);

    Assertions.assertEquals(trx, result);
  }

  @Test
  void getTransactionNotFound() {
    when(transactionInProgressRepository.findById(Mockito.anyString())).thenReturn(Optional.empty());

    ClientException result = Assertions.assertThrows(ClientException.class, () ->
        transactionService.getTransaction(TRANSACTION_ID, USER_ID)
    );

    Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getHttpStatus());
    Assertions.assertEquals("NOT FOUND", ((ClientExceptionNoBody) result).getMessage());
  }

  @Test
  void getTransactionForbidden() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstance(2,
        SyncTrxStatus.IDENTIFIED);
    trx.setUserId(DIFFERENT_USER_ID);

    when(transactionInProgressRepository.findById(Mockito.anyString())).thenReturn(Optional.of(trx));

    ClientException result = Assertions.assertThrows(ClientException.class, () ->
        transactionService.getTransaction(TRANSACTION_ID, USER_ID)
    );

    Assertions.assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());
    Assertions.assertEquals("FORBIDDEN", ((ClientExceptionNoBody) result).getMessage());
  }
}
