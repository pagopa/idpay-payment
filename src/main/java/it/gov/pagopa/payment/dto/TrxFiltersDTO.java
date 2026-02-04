package it.gov.pagopa.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TrxFiltersDTO {
    private String status;
    private String productGtin;
    private String trxCode;
}
