package it.gov.pagopa.payment.connector.event.trx.dto;

import it.gov.pagopa.payment.model.TransactionInProgress;
import java.util.List;
import java.util.Map;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TransactionOutcomeDTO extends TransactionInProgress {

  private Map<String, List<String>> initiativeRejectionReasons;
  private List<String> initiatives;
}
