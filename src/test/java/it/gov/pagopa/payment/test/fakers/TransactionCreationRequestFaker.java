package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

public class TransactionCreationRequestFaker {

  private TransactionCreationRequestFaker() {}

  public static TransactionCreationRequest mockInstance(Integer bias) {
    return mockInstanceBuilder(bias).build();
  }

  public static TransactionCreationRequest.TransactionCreationRequestBuilder mockInstanceBuilder(Integer bias) {
    return TransactionCreationRequest.builder()
        .initiativeId("INITIATIVEID%d".formatted(bias))
        .idTrxAcquirer("IDTRXACQUIRER%s".formatted(bias))
        .amountCents(10L)
        .mcc("MCC%d".formatted(bias));
  }

}
