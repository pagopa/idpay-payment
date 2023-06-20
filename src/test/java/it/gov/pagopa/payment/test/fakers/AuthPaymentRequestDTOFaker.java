package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

public class AuthPaymentRequestDTOFaker {

    public static AuthPaymentRequestDTO mockInstance(Integer bias) {
      return mockInstanceBuilder(bias).build();
    }

    public static AuthPaymentRequestDTO.AuthPaymentRequestDTOBuilder mockInstanceBuilder(Integer bias) {
      return AuthPaymentRequestDTO.builder()
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
          .idTrxAcquirer("IDTRXACQUIRER%d".formatted(bias));
    }

}
