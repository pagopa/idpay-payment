package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class TransactionCreationRequestFaker {

  private TransactionCreationRequestFaker() {}

  public static TransactionCreationRequest mockInstance(Integer bias) {
    return mockInstanceBuilder(bias).build();
  }

  public static TransactionCreationRequest.TransactionCreationRequestBuilder mockInstanceBuilder(Integer bias) {

    Map<String, String> additionalProperties = new HashMap<>();

    additionalProperties.put("description", "Additional description");

    return TransactionCreationRequest.builder()
        .initiativeId("INITIATIVEID%d".formatted(bias))
        .idTrxAcquirer("IDTRXACQUIRER%s".formatted(bias))
        .amountCents(10L)
        .additionalProperties(additionalProperties)
        .mcc("MCC%d".formatted(bias));
  }

}
