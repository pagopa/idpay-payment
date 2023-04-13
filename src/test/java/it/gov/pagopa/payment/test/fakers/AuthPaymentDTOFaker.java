package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import java.util.List;

public class AuthPaymentDTOFaker {

  private AuthPaymentDTOFaker() {}

  public static AuthPaymentDTO mockInstance(Integer bias, TransactionInProgress transaction){
    return mockInstanceBuilder(bias,transaction).build();
  }

  public static AuthPaymentDTO.AuthPaymentDTOBuilder mockInstanceBuilder(Integer bias,
      TransactionInProgress transaction) {
    Reward reward = RewardFaker.mockInstance(bias);
    return AuthPaymentDTO.builder()
        .id(transaction.getId())
        .initiativeId("INITIATIVEID%d".formatted(bias))
        .status(SyncTrxStatus.IDENTIFIED)
        .rejectionReasons(List.of())
        .reward(reward)
        .trxCode("TRXCODE%d".formatted(bias));
  }
}