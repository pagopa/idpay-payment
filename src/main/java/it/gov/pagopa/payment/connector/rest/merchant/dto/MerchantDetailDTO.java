package it.gov.pagopa.payment.connector.rest.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantDetailDTO {
    private String initiativeId;
    private String initiativeName;
    private String businessName; //ragione sociale
    private String legalOfficeAddress;
    private String legalOfficeMunicipality; // comune sede legale
    private String legalOfficeProvince;
    private String legalOfficeZipCode;
    private String certifiedEmail;
    private String fiscalCode;
    private String vatNumber;
    private String status;
    private String iban;
    private LocalDateTime creationDate;
    private LocalDateTime updateDate;
}
