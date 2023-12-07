package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.dto.MerchantTransactionDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class MerchantTransactionDTOFaker {

    public static MerchantTransactionDTO mockInstance(Integer bias, SyncTrxStatus status) {
        return mockInstanceBuilder(bias, status).build();
    }

    public static MerchantTransactionDTO.MerchantTransactionDTOBuilder mockInstanceBuilder(Integer bias,
                                                                                         SyncTrxStatus status) {

        String id = "MOCKEDTRANSACTION_qr-code_%d".formatted(bias);

        Long reward=null;
        if(!status.equals(SyncTrxStatus.CREATED)){
            reward=100L;
        }

        return MerchantTransactionDTO.builder()
                .trxId(id)
                .fiscalCode("MERCHANTFISCALCODE%d".formatted(bias))
                .effectiveAmount(1000L)
                .rewardAmount(reward != null ? reward : Long.valueOf(0))
                .trxCode("trxcode%d".formatted(bias))
                .trxDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .trxExpirationSeconds(CommonUtilities.minutesToSeconds(4320))
                .status(status)
                .channel("CHANNEL%d".formatted(bias))
                .updateDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
    }
}
