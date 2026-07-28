package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.PointOfSaleTransactionDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class PointOfSaleTransactionDTOFaker {

    public static PointOfSaleTransactionDTO mockInstance(Integer bias, SyncTrxStatus status) {
        return mockInstanceBuilder(bias, status).build();
    }

    public static PointOfSaleTransactionDTO.PointOfSaleTransactionDTOBuilder mockInstanceBuilder(Integer bias,
                                                                                           SyncTrxStatus status) {

        String id = "MOCKEDTRANSACTION_qr-code_%d".formatted(bias);

        Long rewardCents=null;
        if(!status.equals(SyncTrxStatus.CREATED)){
            rewardCents=100L;
        }

        return PointOfSaleTransactionDTO.builder()
                .trxId(id)
                .fiscalCode("USERFISCALCODE%d".formatted(bias))
                .effectiveAmountCents(1000L)
                .rewardAmountCents(rewardCents != null ? rewardCents : Long.valueOf(0))
                .trxCode("trxcode%d".formatted(bias))
                .trxDate(OffsetDateTime.now(ZoneId.of("Europe/Rome")).truncatedTo(ChronoUnit.MILLIS))
                .status(status.name())
                .channel("CHANNEL%d".formatted(bias));
    }
}
