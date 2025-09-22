package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeEnrichedResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

public class TransactionBarCodeEnrichedResponseFaker {

    public static TransactionBarCodeEnrichedResponse mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    public static TransactionBarCodeEnrichedResponse.TransactionBarCodeEnrichedResponseBuilder mockInstanceBuilder(Integer bias) {
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        return TransactionBarCodeEnrichedResponse.builder()
                .trxCode("trxcode%d".formatted(bias))
                .initiativeId("INITIATIVEID%d".formatted(bias))
                .trxDate(now)
                .trxEndDate(now.plusDays(10))
                .status(SyncTrxStatus.CREATED);
    }

}
