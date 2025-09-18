package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.ConfirmRequestDTO;

public class ConfirmRequestDTOFaker {

  public static ConfirmRequestDTO mockInstance() {
    return mockInstanceBuilder().build();
  }

  public static ConfirmRequestDTO.ConfirmRequestDTOBuilder mockInstanceBuilder() {
    return ConfirmRequestDTO.builder()
            .trxCode("trxCode")
            .confirmed(true);
  }
}