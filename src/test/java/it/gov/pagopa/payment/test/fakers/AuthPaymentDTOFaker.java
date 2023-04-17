package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import java.math.BigDecimal;
import java.util.List;

public class AuthPaymentDTOFaker {

  private AuthPaymentDTOFaker() {}

  public static AuthPaymentDTO mockInstance(Integer bias, TransactionInProgress transaction){
    return mockInstanceBuilder(bias,transaction).build();
  }

  public static AuthPaymentDTO.AuthPaymentDTOBuilder mockInstanceBuilder(Integer bias,
      TransactionInProgress transaction) {
    return AuthPaymentDTO.builder()
        .id(transaction.getId())
        .initiativeId("INITIATIVEID%d".formatted(bias))
        .status(SyncTrxStatus.IDENTIFIED)
        .rejectionReasons(List.of())
        .amount(BigDecimal.TEN)
        .reward(1000L)
        .trxCode("TRXCODE%d".formatted(bias));
  }
}
