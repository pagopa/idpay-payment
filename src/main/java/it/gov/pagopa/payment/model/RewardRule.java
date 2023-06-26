package it.gov.pagopa.payment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "reward_rule")
public class RewardRule {

  @Id
  private String id;
  private InitiativeConfig initiativeConfig;
}
