package it.gov.pagopa.payment.dto.idpaycode;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class UserRelateResponse {
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

}
