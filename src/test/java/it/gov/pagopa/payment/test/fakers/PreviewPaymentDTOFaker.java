package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.PreviewPaymentDTO;

import java.time.OffsetDateTime;

public class PreviewPaymentDTOFaker {

  private PreviewPaymentDTOFaker() {}

  public static PreviewPaymentDTO mockInstance(){
    return mockInstanceBuilder().build();
  }

  public static PreviewPaymentDTO.PreviewPaymentDTOBuilder mockInstanceBuilder() {
    return PreviewPaymentDTO.builder()
            .userId("userId")
            .trxCode("trxCode")
            .originalAmountCents(700L)
            .rewardCents(100L)
            .residualAmountCents(600L)
            .trxDate(OffsetDateTime.now());
  }
}
