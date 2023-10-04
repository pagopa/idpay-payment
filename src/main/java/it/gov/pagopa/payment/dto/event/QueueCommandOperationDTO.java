package it.gov.pagopa.payment.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class QueueCommandOperationDTO {
    private String operationType;
    private String entityId;
    private LocalDateTime operationTime;
}
