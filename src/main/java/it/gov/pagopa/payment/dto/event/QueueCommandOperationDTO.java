package it.gov.pagopa.payment.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class QueueCommandOperationDTO {
    private String operationType;
    private String entityId;
    private LocalDateTime operationTime;
    private Map<String, String> additionalParams;
}
