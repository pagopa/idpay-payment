package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.enums.OperationType;
import java.time.LocalDateTime;

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
          .trxDate(LocalDateTime.now())
          .trxChargeDate(LocalDateTime.now())
          .amountCents(10L)
          .amountCurrency("AMOUNTCURRENCY%d".formatted(bias))
          .mcc("MCC%d".formatted(bias))
          .acquirerCode("ACQUIRERCODE%d".formatted(bias))
          .acquirerId("ACQUIRERID%d".formatted(bias))
          .idTrxAcquirer("IDTRXACQUIRER%d".formatted(bias))
          .idTrxIssuer("IDTRXISSUER%d".formatted(bias))
          .operationTypeTranscoded(OperationType.CHARGE);
    }

}
