package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.connector.rest.register.dto.ProductListDTO;

public class ProductListDTOFaker {

  private ProductListDTOFaker() {}

  public static ProductListDTO mockInstance(){
    return mockInstanceBuilder().build();
  }

  public static ProductListDTO.ProductListDTOBuilder mockInstanceBuilder() {
    return ProductListDTO.builder();
  }
}
