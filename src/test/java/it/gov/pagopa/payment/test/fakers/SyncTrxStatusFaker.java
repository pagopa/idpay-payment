package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2SyncTrxStatusMapper;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;

public class SyncTrxStatusFaker {
    public static SyncTrxStatusDTO mockInstance(Integer bias, SyncTrxStatus status){
    TransactionInProgress trx= TransactionInProgressFaker.mockInstance(bias, status);
        return new TransactionInProgress2SyncTrxStatusMapper().transactionInProgressMapper(trx);
    }
}
