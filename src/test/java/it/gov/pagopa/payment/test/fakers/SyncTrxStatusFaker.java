package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2SyncTrxStatusMapper;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.utils.RewardConstants;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class SyncTrxStatusFaker {
    public static SyncTrxStatusDTO mockInstance(Integer bias){
    TransactionInProgress trx= TransactionInProgressFaker.mockInstanceBuilder(bias, SyncTrxStatus.IDENTIFIED)
            .authDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
            .reward(0L)
            .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
            .build();
        return new TransactionInProgress2SyncTrxStatusMapper().transactionInProgressMapper(trx);
    }
}
