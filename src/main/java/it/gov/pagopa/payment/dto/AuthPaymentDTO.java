package it.gov.pagopa.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.counters.RewardCounters;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

  @NotBlank
  private String id;
  @NotBlank
  private String trxCode;
  @NotBlank
  private OffsetDateTime trxDate;
  @NotBlank
  private String initiativeId;
  @NotBlank
  private String initiativeName;
  @NotBlank
  private String businessName;
  @NotNull
  private SyncTrxStatus status;
  private Long reward;
  private RewardCounters counters;
  @NotNull
  private List<String> rejectionReasons;
  @NotNull
  private Long amountCents;

  private Long residualBudget;

  @JsonIgnore
  private Map<String, Reward> rewards;

}
