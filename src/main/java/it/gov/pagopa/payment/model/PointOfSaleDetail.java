package it.gov.pagopa.payment.model;

import it.gov.pagopa.payment.enums.PointOfSaleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldNameConstants()
public class PointOfSaleDetail {

  private PointOfSaleType type;
  private String name;

}
