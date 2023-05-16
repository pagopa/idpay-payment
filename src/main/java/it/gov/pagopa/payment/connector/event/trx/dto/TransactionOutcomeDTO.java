package it.gov.pagopa.payment.connector.event.trx.dto;

import it.gov.pagopa.payment.model.TransactionInProgress;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class TransactionOutcomeDTO extends TransactionInProgress {

  private Map<String, List<String>> initiativeRejectionReasons;
}
