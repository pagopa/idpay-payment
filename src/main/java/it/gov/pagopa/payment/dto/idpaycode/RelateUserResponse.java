package it.gov.pagopa.payment.dto.idpaycode;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RelateUserResponse {
    @NotBlank
    private String id;
    @NotNull
    private SyncTrxStatus status;

}
