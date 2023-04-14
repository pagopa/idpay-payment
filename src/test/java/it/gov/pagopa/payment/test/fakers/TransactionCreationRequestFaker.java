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
        .senderCode("SENDERCODE%d".formatted(bias))
        .merchantFiscalCode("MERCHANTFISCALCODE%d".formatted(bias))
        .vat("VAT%d".formatted(bias))
        .trxDate(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS))
        .amountCents(10L)
        .amountCurrency("AMOUNTCURRENCY%d".formatted(bias))
        .mcc("MCC%d".formatted(bias))
        .acquirerCode("ACQUIRERCODE%d".formatted(bias))
        .acquirerId("ACQUIRERID%d".formatted(bias))
        .idTrxIssuer("IDTRXISSUER%d".formatted(bias))
        .idTrxAcquirer("IDTRXACQUIRER%d".formatted(bias))
        .callbackUrl("CALLBACKURL%d".formatted(bias))
        .idTrxIssuer("IDTRXISSUER%d".formatted(bias));
  }

}
