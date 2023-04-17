package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

public class TransactionResponseFaker {

    public static TransactionResponse mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    public static TransactionResponse.TransactionResponseBuilder mockInstanceBuilder(Integer bias) {
        return TransactionResponse.builder()
                .initiativeId("INITIATIVEID%d".formatted(bias))
                .senderCode("SENDERCODE%d".formatted(bias))
                .trxDate(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .amountCents(10L)
                .amountCurrency("AMOUNTCURRENCY%d".formatted(bias))
                .mcc("MCC%d".formatted(bias))
                .acquirerCode("ACQUIRERCODE%d".formatted(bias))
                .acquirerId("ACQUIRERID%d".formatted(bias))
                .idTrxAcquirer("IDTRXACQUIRER%d".formatted(bias))
                .idTrxIssuer("IDTRXISSUER%d".formatted(bias))
                .trxCode("TRXCODE%d".formatted(bias))
                .status(SyncTrxStatus.CREATED);
    }

}
