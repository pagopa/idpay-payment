package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2SyncTrxStatusMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.mockito.Mock;

public class SyncTrxStatusFaker {
    @Mock
    private static TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapperMock;

    private static final TransactionInProgress2SyncTrxStatusMapper transactionMapper= new TransactionInProgress2SyncTrxStatusMapper(transactionInProgress2TransactionResponseMapperMock);

    public static SyncTrxStatusDTO mockInstance(Integer bias, SyncTrxStatus status){
    TransactionInProgress trx= TransactionInProgressFaker.mockInstance(bias, status);
        return transactionMapper.transactionInProgressMapper(trx);
    }
}
