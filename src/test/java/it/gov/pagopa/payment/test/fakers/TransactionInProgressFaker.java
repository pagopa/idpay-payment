package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.enums.Status;
import it.gov.pagopa.payment.model.TransactionInProgress;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionInProgressFaker {
  public static TransactionInProgress mockInstance(Integer bias) {
    return mockInstanceBuilder(bias).build();
  }

  public static TransactionInProgress.TransactionInProgressBuilder mockInstanceBuilder(Integer bias) {
    return TransactionInProgress.builder()
        .initiativeId("INITIATIVEID%d".formatted(bias))
        .senderCode("SENDERCODE%d".formatted(bias))
        .merchantFiscalCode("MERCHANTFISCALCODE%d".formatted(bias))
        .vat("VAT%d".formatted(bias))
        .trxDate(LocalDateTime.now())
        .amount(BigDecimal.TEN)
        .amountCurrency("AMOUNTCURRENCY%d".formatted(bias))
        .mcc("MCC%d".formatted(bias))
        .acquirerCode("ACQUIRERCODE%d".formatted(bias))
        .acquirerId("ACQUIRERID%d".formatted(bias))
        .idTrxAcquirer("IDTRXACQUIRER%d".formatted(bias))
        .callbackUrl("CALLBACKURL%d".formatted(bias))
        .idTrxIssuer("IDTRXISSUER%d".formatted(bias))
        .trxCode("TRXCODE%d".formatted(bias))
        .status(Status.CREATED);
  }
}
