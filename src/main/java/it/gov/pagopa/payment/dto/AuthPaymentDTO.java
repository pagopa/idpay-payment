package it.gov.pagopa.payment.dto;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthPaymentDTO {

  @NotEmpty
  private String id;
  @NotEmpty
  private String trxCode;
  @NotEmpty
  private String initiativeId;
  @NotNull
  private SyncTrxStatus status;
  @NotNull
  private Long reward;
  @NotNull
  private List<String> rejectionReasons;
  @NotNull
  private Long amountCents;

}
