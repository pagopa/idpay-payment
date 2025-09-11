package it.gov.pagopa.payment.connector.rest.register.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductRequestDTO {

    private String role;
    private String organizationId;
    private String productName;
    private String productFileId;
    private String eprelCode;
    private String gtinCode;
    private ProductStatus status;
    private ProductCategories category;
    private Pageable pageable;

}
