package it.gov.pagopa.payment.connector.rest.reward.dto;

import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.OperationType;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthPaymentResponseDTO {

  private String transactionId;
  private String channel;
  private String initiativeId;
  private String userId;
  private SyncTrxStatus status;
  private Reward reward;
  private List<String> rejectionReasons;
  private BigDecimal effectiveAmount;
  private BigDecimal amount;
  private Long amountCents;
  private OperationType operationType;
  private long counterVersion;
}
