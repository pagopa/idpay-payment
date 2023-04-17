package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import java.math.BigDecimal;
import java.util.List;

public class AuthPaymentResponseDTOFaker {
  public static AuthPaymentResponseDTO mockInstance(Integer bias, SyncTrxStatus status) {
    return mockInstanceBuilder(bias, status).build();
  }

  public static AuthPaymentResponseDTO.AuthPaymentResponseDTOBuilder mockInstanceBuilder(Integer bias, SyncTrxStatus status) {
    Reward reward = RewardFaker.mockInstance(bias);
    return AuthPaymentResponseDTO.builder()
        .transactionId("TRANSACTION%d_qr-code".formatted(bias))
        .initiativeId("INITIATIVEID%d".formatted(bias))
        .userId("USERID%d".formatted(bias))
        .effectiveAmount(BigDecimal.TEN)
        .reward(reward)
        .rejectionReasons(List.of())
        .status(status);
  }
}