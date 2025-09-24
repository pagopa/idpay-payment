package it.gov.pagopa.payment.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CancelTransactionAuditDTO {
  private String initiativeId;
  private String trxId;
  private String trxCode;
  private String userId;
  private Long rewardCents;
  private List<String> rejectionReasons;
  private String merchantId;
  private String pointOfSaleId;
}