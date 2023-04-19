package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.utils.RewardConstants;
import it.gov.pagopa.payment.utils.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class SyncTrxStatusFaker {

    public static SyncTrxStatusDTO mockInstance(Integer bias){
        return mockInstanceBuilder(bias).build();
    }
    public static SyncTrxStatusDTO.SyncTrxStatusDTOBuilder mockInstanceBuilder(Integer bias){

        return SyncTrxStatusDTO.builder()
                .id("TRANSACTIONID%d".formatted(bias))
                .idTrxIssuer("IDTRXISSUER%d".formatted(bias))
                .trxCode("TRXCODE%d".formatted(bias))
                .trxDate(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .authDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .operationType(PaymentConstants.OPERATION_TYPE_CHARGE)
                .amountCents(10L)
                .amountCurrency("AMOUNTCURRENCY%d".formatted(bias))
                .mcc("MCC%d".formatted(bias))
                .acquirerId("ACQUIRERID%d".formatted(bias))
                .merchantId("MERCHANTID%d".formatted(bias))
                .initiativeId("INITIATIVEID%d".formatted(bias))
                .rewardCents(Utils.euroToCents(BigDecimal.valueOf(100L).setScale(2, RoundingMode.UNNECESSARY)))
                .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                .status(it.gov.pagopa.payment.enums.SyncTrxStatus.AUTHORIZED);
    }
}
