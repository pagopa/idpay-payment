package it.gov.pagopa.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PointOfSaleTransactionsListDTO {

    private List<PointOfSaleTransactionDTO> content;
    private int pageNo;
    private int pageSize;
    private int totalElements;
    private int totalPages;
}
