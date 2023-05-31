package it.gov.pagopa.payment.dto;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
  @NotNull
  private List<String> rejectionReasons;
  @NotNull
  private Long amountCents;

}
