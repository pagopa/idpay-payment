package it.gov.pagopa.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionAuditDTO {
  private String initiativeId;
  private String trxId;
  private String trxCode;
  private String userId;
  private Long rewardCents;
  private String invoiceFilename;
  private String docNumber;
  private String merchantId;
  private String pointOfSaleId;
}