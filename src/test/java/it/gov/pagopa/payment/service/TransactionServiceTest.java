package it.gov.pagopa.payment.service;

import static org.mockito.Mockito.when;

import it.gov.pagopa.common.web.exception.custom.ForbiddenException;
import it.gov.pagopa.common.web.exception.custom.NotFoundException;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2SyncTrxStatusMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2SyncTrxStatusMapperTest;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
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

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

  @Mock
  private TransactionInProgressRepository transactionInProgressRepository;

  private TransactionService transactionService;

  private final TransactionInProgress2SyncTrxStatusMapper transaction2statusMapper = new TransactionInProgress2SyncTrxStatusMapper();

  private static final String TRANSACTION_ID = "MOCKEDTRANSACTION_qr-code_1";

  private static final String USER_ID = "USERID1";
  private static final String DIFFERENT_USER_ID = "USERID2";

  @BeforeEach
  void setUp() {
    transactionService = new TransactionServiceImpl(transactionInProgressRepository, transaction2statusMapper);
  }

  @Test
  void getTransaction() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstanceBuilder(1, SyncTrxStatus.IDENTIFIED)
            .userId(USER_ID)
            .reward(0L)
            .build();

    when(transactionInProgressRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trx));

    SyncTrxStatusDTO result = transactionService.getTransaction(TRANSACTION_ID, USER_ID);

    Assertions.assertNotNull(result);
    TransactionInProgress2SyncTrxStatusMapperTest.mapperAssertion(trx, result);
  }

  @Test
  void getTransactionNotFound() {
    when(transactionInProgressRepository.findById(Mockito.anyString())).thenReturn(Optional.empty());

    NotFoundException result = Assertions.assertThrows(NotFoundException.class, () ->
        transactionService.getTransaction(TRANSACTION_ID, USER_ID)
    );

    Assertions.assertEquals("NOT FOUND", result.getCode());
    Assertions.assertEquals("Transaction not found", result.getMessage());
  }

  @Test
  void getTransactionForbidden() {
    TransactionInProgress trx = TransactionInProgressFaker.mockInstanceBuilder(1, SyncTrxStatus.IDENTIFIED)
            .userId(DIFFERENT_USER_ID)
            .build();

    when(transactionInProgressRepository.findById(Mockito.anyString())).thenReturn(Optional.of(trx));

    ForbiddenException result = Assertions.assertThrows(ForbiddenException.class, () ->
        transactionService.getTransaction(TRANSACTION_ID, USER_ID)
    );

    Assertions.assertEquals("User does not exist", result.getMessage());
    Assertions.assertEquals("FORBIDDEN", result.getCode());
  }
}
