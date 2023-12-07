package it.gov.pagopa.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.counters.RewardCounters;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthPaymentDTO {

  private String id;
  private String trxCode;
  private OffsetDateTime trxDate;
  private String initiativeId;
  private String initiativeName;
  private String businessName;
  private SyncTrxStatus status;
  private Long reward;
  private RewardCounters counters;
  private List<String> rejectionReasons;
  private Long amountCents;

  private Long residualBudget;

  @JsonIgnore
  private Map<String, Reward> rewards;

}
