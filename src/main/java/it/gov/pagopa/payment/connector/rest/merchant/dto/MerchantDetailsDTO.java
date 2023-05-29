package it.gov.pagopa.payment.connector.rest.merchant.dto;

import lombok.Data;

@Data
public class MerchantDetailsDTO {
    private String fiscalCode;
    private String vatNumber;
}
