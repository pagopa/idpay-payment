package it.gov.pagopa.payment.connector.rest.reward.dto;

import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
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

  String transactionId;
  String initiativeId;
  String userId;
  SyncTrxStatus status;
  Reward reward;
  List<String> rejectionReasons;

}
