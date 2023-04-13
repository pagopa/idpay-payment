package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class TransactionInProgressFaker {
  public static TransactionInProgress mockInstance(Integer bias) {
    return mockInstanceBuilder(bias).build();
  }

  public static TransactionInProgress.TransactionInProgressBuilder mockInstanceBuilder(Integer bias) {

    String id = "MOCKEDTRANSACTION_qr-code_%d".formatted(bias);

    return TransactionInProgress.builder()
        .id(id)
        .correlationId(id)
        .initiativeId("INITIATIVEID%d".formatted(bias))
        .senderCode("SENDERCODE%d".formatted(bias))
        .merchantId("MERCHANTID%d".formatted(bias))
        .merchantFiscalCode("MERCHANTFISCALCODE%d".formatted(bias))
        .vat("VAT%d".formatted(bias))
        .trxDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
        .trxChargeDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
        .amountCents(1000L)
        .effectiveAmount(BigDecimal.TEN.setScale(2, RoundingMode.UNNECESSARY))
        .amountCurrency("AMOUNTCURRENCY%d".formatted(bias))
        .mcc("MCC%d".formatted(bias))
        .acquirerCode("ACQUIRERCODE%d".formatted(bias))
        .acquirerId("ACQUIRERID%d".formatted(bias))
        .idTrxAcquirer("IDTRXACQUIRER%d".formatted(bias))
        .callbackUrl("CALLBACKURL%d".formatted(bias))
        .idTrxIssuer("IDTRXISSUER%d".formatted(bias))
        .trxCode("TRXCODE%d".formatted(bias))
        .operationType(PaymentConstants.OPERATION_TYPE_CHARGE)
        .operationTypeTranscoded(OperationType.CHARGE)
        .status(SyncTrxStatus.CREATED);
  }
}
