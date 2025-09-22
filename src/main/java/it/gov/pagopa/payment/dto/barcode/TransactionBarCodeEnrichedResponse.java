package it.gov.pagopa.payment.dto.barcode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class TransactionBarCodeEnrichedResponse extends TransactionBarCodeResponse{
    private OffsetDateTime trxEndDate;
}
