package it.gov.pagopa.payment.connector.rest.register.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDTO {

    private String gtinCode;
    private String organizationId;
    private LocalDateTime registrationDate;
    private String status;
    private String model;
    private String productGroup;
    private String category;
    private String brand;
    private String eprelCode;
    private String productCode;
    private String countryOfProduction;
    private String energyClass;
    private String linkEprel;
    private String batchName;
    private String productName;
    private String capacity;
    private List<StatusChangeEvent> statusChangeChronology;
    private String organizationName;
}