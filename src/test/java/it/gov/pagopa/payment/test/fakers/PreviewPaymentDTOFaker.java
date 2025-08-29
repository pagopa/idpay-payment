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
            .amountCents(100L)
            .trxDate(OffsetDateTime.now())
            .rewardCents(100L);
  }
}
