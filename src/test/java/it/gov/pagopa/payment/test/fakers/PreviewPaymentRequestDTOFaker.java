package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.PreviewPaymentRequestDTO;

import java.math.BigDecimal;

public class PreviewPaymentRequestDTOFaker {

  private PreviewPaymentRequestDTOFaker() {}

  public static PreviewPaymentRequestDTO mockInstance(){
    return mockInstanceBuilder().build();
  }

  public static PreviewPaymentRequestDTO.PreviewPaymentRequestDTOBuilder mockInstanceBuilder() {
    return PreviewPaymentRequestDTO.builder()
            .amount(BigDecimal.valueOf(100))
            .product("product");
  }

}
