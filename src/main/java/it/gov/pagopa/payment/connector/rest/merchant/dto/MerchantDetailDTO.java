package it.gov.pagopa.payment.connector.rest.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantDetailDTO {
    private String initiativeName;
    private String businessName;
    private String fiscalCode;
    private String vatNumber;
}
