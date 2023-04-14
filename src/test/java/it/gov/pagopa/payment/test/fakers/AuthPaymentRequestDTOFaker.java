package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.enums.OperationType;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class AuthPaymentRequestDTOFaker {

    public static AuthPaymentRequestDTO mockInstance(Integer bias) {
      return mockInstanceBuilder(bias).build();
    }

    public static AuthPaymentRequestDTO.AuthPaymentRequestDTOBuilder mockInstanceBuilder(Integer bias) {
      return AuthPaymentRequestDTO.builder()
          .transactionId("TRANSACTION%d_qr-code".formatted(bias))
          .senderCode("SENDERCODE%d".formatted(bias))
          .merchantId("MERCHANTID%d".formatted(bias))
          .merchantFiscalCode("MERCHANTFISCALCODE%d".formatted(bias))
          .vat("VAT%d".formatted(bias))
          .trxDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
          .trxChargeDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
          .amountCents(10L)
          .amountCurrency("AMOUNTCURRENCY%d".formatted(bias))
          .mcc("MCC%d".formatted(bias))
          .acquirerCode("ACQUIRERCODE%d".formatted(bias))
          .acquirerId("ACQUIRERID%d".formatted(bias))
          .idTrxAcquirer("IDTRXACQUIRER%d".formatted(bias))
          .idTrxIssuer("IDTRXISSUER%d".formatted(bias))
          .operationType(OperationType.CHARGE);
    }

}
