package it.gov.pagopa.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RevertTransactionAuditDTO {
  private String initiativeId;
  private String trxId;
  private String trxCode;
  private String userId;
  private Long rewardCents;
  private String creditNoteFilename;
  private String creditNoteNumber;
  private String merchantId;
  private String pointOfSaleId;
}