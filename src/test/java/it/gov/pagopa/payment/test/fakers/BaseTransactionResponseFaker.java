package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.common.BaseTransactionResponseDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

public class BaseTransactionResponseFaker {

    public static BaseTransactionResponseDTO mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    public static BaseTransactionResponseDTO.BaseTransactionResponseDTOBuilder<?,?> mockInstanceBuilder(Integer bias) {
        return BaseTransactionResponseDTO.builder()
                .initiativeId("INITIATIVEID%d".formatted(bias))
                .trxDate(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .amountCents(10L)
                .amountCurrency("AMOUNTCURRENCY%d".formatted(bias))
                .mcc("MCC%d".formatted(bias))
                .acquirerId("ACQUIRERID%d".formatted(bias))
                .idTrxAcquirer("IDTRXACQUIRER%d".formatted(bias))
                .idTrxIssuer("IDTRXISSUER%d".formatted(bias))
                .trxCode("trxcode%d".formatted(bias))
                .status(SyncTrxStatus.CREATED);
    }

}
