package it.gov.pagopa.payment.service.payment;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.payment.configuration.AppConfigurationProperties;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.ExpirationStatusUpdateException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.TrxCodeGenUtil;
import org.bson.BsonString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionInProgressServiceImplTest {
    @Mock private TransactionInProgressRepository transactionInProgressRepositoryMock;
    @Mock private TrxCodeGenUtil trxCodeGenUtilMock;
    @Mock private TransactionNotifierService transactionNotifierServiceMock;
    @Mock private AppConfigurationProperties.ExtendedTransactions extendedTransactionsMock;

    private TransactionInProgressService transactionInProgressService;

    @BeforeEach
    void setUp() {
        transactionInProgressService = new TransactionInProgressServiceImpl(
                transactionInProgressRepositoryMock,
                trxCodeGenUtilMock,
                transactionNotifierServiceMock,
                extendedTransactionsMock);
    }

    @Test
    void generateTrxCodeAndSave() {
        AtomicInteger[] count = {new AtomicInteger(0)};
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        when(trxCodeGenUtilMock.get()).thenReturn("trxcode1");

        when(transactionInProgressRepositoryMock.createIfExists(trx, "trxcode1"))
                .thenAnswer(a -> {
                    a.getArguments();
                    if(count[0].get() < 1){
                        count[0].incrementAndGet();
                        return UpdateResult.acknowledged(1L, 0L, null);
                    }
                    return UpdateResult.acknowledged(0L, 0L, new BsonString(trx.getId()));
                });

        transactionInProgressService.generateTrxCodeAndSave(trx, "DUMMY_FLOW_NAME");

        Mockito.verifyNoMoreInteractions(trxCodeGenUtilMock, transactionInProgressRepositoryMock);

    }

    @Test
    void extendedTransactionExpireUpdateShouldReturnUpdateNumberOnRequest() {;
        when(transactionInProgressRepositoryMock.updateStatusForExpiredVoucherTransactions(eq("INIT1")))
                .thenReturn(1L);
        long expiredTransactionsProcessed =
                Assertions.assertDoesNotThrow(() ->
                        transactionInProgressService.findAndUpdateExpiredTransactionsStatus("INIT1"));
        Assertions.assertEquals(1L, expiredTransactionsProcessed);
        verify(transactionInProgressRepositoryMock).updateStatusForExpiredVoucherTransactions(eq("INIT1"));
    }

    @Test
    void extendedTransactionExpireUpdateShouldReturnManagedExceptionOnError() {;
        when(transactionInProgressRepositoryMock.updateStatusForExpiredVoucherTransactions(eq("INIT1")))
                .thenThrow(new RuntimeException());
        Assertions.assertThrows(ExpirationStatusUpdateException.class, () ->
                        transactionInProgressService.findAndUpdateExpiredTransactionsStatus("INIT1"));
        verify(transactionInProgressRepositoryMock).updateStatusForExpiredVoucherTransactions(eq("INIT1"));
    }

    @Test
    void extendedTransactionStaleExpiredResendShouldProcessAndSendDataOnSinglePage() {;
        when(extendedTransactionsMock.getSendExpiredSendBatchSize()).thenReturn(1);
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.EXPIRED);
        when(transactionInProgressRepositoryMock.findUnprocessedExpiredVoucherTransactions(
                any(), eq(1), any())).thenReturn(Collections.emptyList());
        when(transactionInProgressRepositoryMock.findUnprocessedExpiredVoucherTransactions(
                eq("INIT1"), eq(1), eq(0)))
                .thenReturn(Collections.singletonList(trx));
        when(transactionNotifierServiceMock.notify(any(),any())).thenReturn(true);
        long expiredTransactionsProcessed =
                Assertions.assertDoesNotThrow(() ->
                        transactionInProgressService.sendEventForStaleExpiredTransactions("INIT1"));
        Assertions.assertEquals(1L, expiredTransactionsProcessed);
        verify(transactionInProgressRepositoryMock).findUnprocessedExpiredVoucherTransactions(
                eq("INIT1"), eq(1), eq(0));
    }

    @Test
    void extendedTransactionStaleExpiredResendShouldProcessAndSendDataOnMultiplePages() {;
        when(extendedTransactionsMock.getSendExpiredSendBatchSize()).thenReturn(1);
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.EXPIRED);
        when(transactionInProgressRepositoryMock.findUnprocessedExpiredVoucherTransactions(
                any(), eq(1), any())).thenReturn(Collections.emptyList());
        when(transactionInProgressRepositoryMock.findUnprocessedExpiredVoucherTransactions(
                eq("INIT1"), eq(1), eq(0)))
                .thenReturn(Collections.singletonList(trx));
        when(transactionInProgressRepositoryMock.findUnprocessedExpiredVoucherTransactions(
                eq("INIT1"), eq(1), eq(1)))
                .thenReturn(Collections.singletonList(trx));
        when(transactionNotifierServiceMock.notify(any(),any())).thenReturn(true);
        long expiredTransactionsProcessed =
                Assertions.assertDoesNotThrow(() ->
                        transactionInProgressService.sendEventForStaleExpiredTransactions("INIT1"));
        Assertions.assertEquals(2L, expiredTransactionsProcessed);
        verify(transactionInProgressRepositoryMock).findUnprocessedExpiredVoucherTransactions(
                eq("INIT1"), eq(1), eq(0));
        verify(transactionInProgressRepositoryMock).findUnprocessedExpiredVoucherTransactions(
                eq("INIT1"), eq(1), eq(1));
    }


}