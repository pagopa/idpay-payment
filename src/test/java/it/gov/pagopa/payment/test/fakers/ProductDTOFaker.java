package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.connector.rest.register.dto.ProductDTO;

public class ProductDTOFaker {

  private ProductDTOFaker() {}

  public static ProductDTO mockInstance(){
    return mockInstanceBuilder().build();
  }

  public static ProductDTO.ProductDTOBuilder mockInstanceBuilder() {
    return ProductDTO.builder()
            .category("category")
            .categoryCode("category_code")
            .gtinCode("gtin")
            .productName("productName");
  }
}
