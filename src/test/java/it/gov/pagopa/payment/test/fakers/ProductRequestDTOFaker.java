package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.connector.rest.register.dto.ProductRequestDTO;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductStatus;

public class ProductRequestDTOFaker {

  private ProductRequestDTOFaker() {}

  public static ProductRequestDTO mockInstance(){
    return mockInstanceBuilder().build();
  }

  public static ProductRequestDTO.ProductRequestDTOBuilder mockInstanceBuilder() {
    return ProductRequestDTO.builder()
            .productFileId("productField")
            .role("role")
            .eprelCode("eprelCode")
            .productName("productName")
            .status(ProductStatus.APPROVED)
            .gtinCode("gtinCode")
            .organizationId("organizationId")
            .pageable(null);
  }
}
