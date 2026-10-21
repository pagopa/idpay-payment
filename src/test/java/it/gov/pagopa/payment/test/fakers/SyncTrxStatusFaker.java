package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.mapper.TransactionMapper;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import org.mockito.Mock;

public class SyncTrxStatusFaker {
    @Mock
    private static TransactionMapper transactionMapper;

    public static SyncTrxStatusDTO mockInstance(Integer bias, SyncTrxStatus status){
        Transaction trx = TransactionFaker.mockInstance(bias, status);
        return transactionMapper.transactionToSyncTrxStatus(trx);
    }
}
