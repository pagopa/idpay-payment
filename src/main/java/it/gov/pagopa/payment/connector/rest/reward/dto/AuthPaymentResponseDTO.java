package it.gov.pagopa.payment.connector.rest.reward.dto;

import it.gov.pagopa.payment.dto.Reward;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthPaymentResponseDTO {

  String transactionId;
  String initiativeId;
  String userId;
  SyncTrxStatus status;
  Reward reward;
  List<String> rejectionReasons;

}
