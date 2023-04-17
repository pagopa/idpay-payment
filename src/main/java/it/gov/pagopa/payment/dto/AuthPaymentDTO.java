package it.gov.pagopa.payment.dto;

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
public class AuthPaymentDTO {

  private String id;
  private String trxCode;
  private String initiativeId;
  private SyncTrxStatus status;
  private Reward reward;
  private List<String> rejectionReasons;
  private Long amountCents;

}
