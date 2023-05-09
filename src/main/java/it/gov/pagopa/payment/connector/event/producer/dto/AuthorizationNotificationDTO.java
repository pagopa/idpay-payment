package it.gov.pagopa.payment.connector.event.producer.dto;

import it.gov.pagopa.payment.enums.SyncTrxStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Data
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
    private LocalDateTime authorizationDateTime;
}
