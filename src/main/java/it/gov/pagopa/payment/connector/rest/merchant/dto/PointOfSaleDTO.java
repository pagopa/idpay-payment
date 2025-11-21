package it.gov.pagopa.payment.connector.rest.merchant.dto;

import it.gov.pagopa.payment.enums.PointOfSaleTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointOfSaleDTO {

  private PointOfSaleTypeEnum type;
  private String franchiseName;
  private String businessName;
  private String fiscalCode;
  private String vatNumber;
}
