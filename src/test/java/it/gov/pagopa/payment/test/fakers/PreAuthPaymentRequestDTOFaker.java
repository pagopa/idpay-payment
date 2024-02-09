package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.connector.rest.reward.dto.PreAuthPaymentRequestDTO;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

public class PreAuthPaymentRequestDTOFaker {

    public static PreAuthPaymentRequestDTO mockInstance(Integer bias) {
      return mockInstanceBuilder(bias).build();
    }
    public static PreAuthPaymentRequestDTO.PreAuthPaymentRequestDTOBuilder mockInstanceBuilder(Integer bias) {
      return PreAuthPaymentRequestDTO.builder()
          .transactionId("TRANSACTION%d_qr-code".formatted(bias))
          .merchantId("MERCHANTID%d".formatted(bias))
          .merchantFiscalCode("MERCHANTFISCALCODE%d".formatted(bias))
          .vat("VAT%d".formatted(bias))
          .trxDate(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS))
          .trxChargeDate(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS))
          .amountCents(10L)
          .amountCurrency("AMOUNTCURRENCY%d".formatted(bias))
          .mcc("MCC%d".formatted(bias))
          .acquirerId("ACQUIRERID%d".formatted(bias))
          .idTrxAcquirer("IDTRXACQUIRER%d".formatted(bias))
          .idTrxIssuer("IDTRXISSUER%d".formatted(bias));
    }

}
