package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import java.time.LocalDateTime;

public class TransactionCreatedFaker {

  public static TransactionResponse mockInstance(Integer bias) {
    return mockInstanceBuilder(bias).build();
  }

  public static TransactionResponse.TransactionResponseBuilder mockInstanceBuilder(Integer bias) {
    return TransactionResponse.builder()
        .initiativeId("INITIATIVEID%d".formatted(bias))
        .senderCode("SENDERCODE%d".formatted(bias))
        .trxDate(LocalDateTime.now())
        .amountCents(10L)
        .amountCurrency("AMOUNTCURRENCY%d".formatted(bias))
        .mcc("MCC%d".formatted(bias))
        .acquirerCode("ACQUIRERCODE%d".formatted(bias))
        .acquirerId("ACQUIRERID%d".formatted(bias))
        .idTrxAcquirer("IDTRXACQUIRER%d".formatted(bias))
        .idTrxIssuer("IDTRXISSUER%d".formatted(bias))
        .trxCode("TRXCODE%d".formatted(bias));
  }

}
