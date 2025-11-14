package it.gov.pagopa.payment.service.payment.common;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommonCancelServiceBatchTest {

  @Mock
  private TransactionInProgressRepository repositoryMock;

  @Mock
  private CommonCancelServiceImpl service;

  private CommonCancelServiceBatchImpl serviceBatch;

  @BeforeEach
  void init() {

    serviceBatch = new CommonCancelServiceBatchImpl(
        service, repositoryMock
    );
  }

  @Test
  void testRejectPendingTransactions_ok() {
    TransactionInProgress trx1 = TransactionInProgressFaker.mockInstance(0,
        SyncTrxStatus.AUTHORIZED);
    TransactionInProgress trx2 = TransactionInProgressFaker.mockInstance(1,
        SyncTrxStatus.AUTHORIZED);

    when(repositoryMock.findPendingTransactions(100))
        .thenReturn(List.of(trx1, trx2))
        .thenReturn(List.of());

    CommonCancelServiceBatchImpl spyServiceBatch = Mockito.spy(serviceBatch);
    Mockito.doNothing().when(service).cancelTransaction(
        Mockito.anyString(),
        Mockito.anyString(),
        Mockito.anyString(),
        Mockito.anyString()
    );

    spyServiceBatch.rejectPendingTransactions();

    verify(repositoryMock, Mockito.times(2)).findPendingTransactions(100);
    verify(service, Mockito.times(2))
        .cancelTransaction(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
            Mockito.anyString());
  }

}
