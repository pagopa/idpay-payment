package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

public class TransactionBarCodeResponseFaker {

    public static TransactionBarCodeResponse mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    public static TransactionBarCodeResponse.TransactionBarCodeResponseBuilder mockInstanceBuilder(Integer bias) {
        return TransactionBarCodeResponse.builder()
                .trxCode("trxcode%d".formatted(bias))
                .initiativeId("INITIATIVEID%d".formatted(bias))
                .trxDate(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .status(SyncTrxStatus.CREATED);
    }

}
