package it.gov.pagopa.payment.connector.event.producer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthorizationNotificationDTO {
    private String operationType;
    private String trxId;
    private String initiativeId;
    private String userId;
    private OffsetDateTime trxDate;
    private String merchantId;
    private String merchantFiscalCode;
    private SyncTrxStatus status;
    private Long reward;
    private Long amountCents;
    private List<String> rejectionReasons;
}
