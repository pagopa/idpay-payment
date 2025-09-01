package it.gov.pagopa.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreviewPaymentDTO {

  private String trxCode;
  private OffsetDateTime trxDate;
  private SyncTrxStatus status;
  private Long rewardCents;
  private Long amountCents;
  private String userId;
  private String product;


}
