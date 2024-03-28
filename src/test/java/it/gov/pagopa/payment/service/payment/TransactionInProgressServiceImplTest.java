package it.gov.pagopa.payment.service.payment;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.TrxCodeGenUtil;
import org.bson.BsonString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionInProgressServiceImplTest {
    @Mock private TransactionInProgressRepository transactionInProgressRepositoryMock;
    @Mock private TrxCodeGenUtil trxCodeGenUtilMock;

    private TransactionInProgressService transactionInProgressService;

    @BeforeEach
    void setUp() {
        transactionInProgressService = new TransactionInProgressServiceImpl(transactionInProgressRepositoryMock, trxCodeGenUtilMock);
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
}