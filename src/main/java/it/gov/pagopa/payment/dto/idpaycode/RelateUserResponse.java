package it.gov.pagopa.payment.dto.idpaycode;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
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
    private String id;
    private SyncTrxStatus status;

}
